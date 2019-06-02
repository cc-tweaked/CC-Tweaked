/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.client.render;

import net.minecraft.block.BlockState;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.render.model.json.ModelItemPropertyOverrideList;
import net.minecraft.client.render.model.json.ModelTransformation;
import net.minecraft.client.texture.Sprite;
import net.minecraft.util.math.Direction;

import javax.annotation.Nonnull;
import javax.vecmath.Matrix4f;
import java.util.*;

public class TurtleMultiModel implements BakedModel
{
    private final BakedModel m_baseModel;
    private final BakedModel m_overlayModel;
    private final Matrix4f m_generalTransform;
    private final BakedModel m_leftUpgradeModel;
    private final Matrix4f m_leftUpgradeTransform;
    private final BakedModel m_rightUpgradeModel;
    private final Matrix4f m_rightUpgradeTransform;
    private List<BakedQuad> m_generalQuads = null;
    private Map<Direction, List<BakedQuad>> m_faceQuads = new EnumMap<>( Direction.class );

    public TurtleMultiModel( BakedModel baseModel, BakedModel overlayModel, Matrix4f generalTransform, BakedModel leftUpgradeModel, Matrix4f leftUpgradeTransform, BakedModel rightUpgradeModel, Matrix4f rightUpgradeTransform )
    {
        // Get the models
        m_baseModel = baseModel;
        m_overlayModel = overlayModel;
        m_leftUpgradeModel = leftUpgradeModel;
        m_leftUpgradeTransform = leftUpgradeTransform;
        m_rightUpgradeModel = rightUpgradeModel;
        m_rightUpgradeTransform = rightUpgradeTransform;
        m_generalTransform = generalTransform;
    }

    @Nonnull
    @Override
    public List<BakedQuad> getQuads( BlockState state, Direction side, @Nonnull Random rand )
    {
        if( side != null )
        {
            if( !m_faceQuads.containsKey( side ) ) m_faceQuads.put( side, buildQuads( state, side, rand ) );
            return m_faceQuads.get( side );
        }
        else
        {
            if( m_generalQuads == null ) m_generalQuads = buildQuads( state, side, rand );
            return m_generalQuads;
        }
    }

    private List<BakedQuad> buildQuads( BlockState state, Direction side, Random rand )
    {
        ArrayList<BakedQuad> quads = new ArrayList<>();
        ModelTransformer.transformQuadsTo( quads, m_baseModel.getQuads( state, side, rand ), m_generalTransform );
        if( m_overlayModel != null )
        {
            ModelTransformer.transformQuadsTo( quads, m_overlayModel.getQuads( state, side, rand ), m_generalTransform );
        }
        if( m_leftUpgradeModel != null )
        {
            Matrix4f upgradeTransform = m_generalTransform;
            if( m_leftUpgradeTransform != null )
            {
                upgradeTransform = new Matrix4f( m_generalTransform );
                upgradeTransform.mul( m_leftUpgradeTransform );
            }
            ModelTransformer.transformQuadsTo( quads, m_leftUpgradeModel.getQuads( state, side, rand ), upgradeTransform );
        }
        if( m_rightUpgradeModel != null )
        {
            Matrix4f upgradeTransform = m_generalTransform;
            if( m_rightUpgradeTransform != null )
            {
                upgradeTransform = new Matrix4f( m_generalTransform );
                upgradeTransform.mul( m_rightUpgradeTransform );
            }
            ModelTransformer.transformQuadsTo( quads, m_rightUpgradeModel.getQuads( state, side, rand ), upgradeTransform );
        }
        quads.trimToSize();
        return quads;
    }

    @Override
    public boolean useAmbientOcclusion()
    {
        return m_baseModel.useAmbientOcclusion();
    }

    @Override
    public boolean hasDepthInGui()
    {
        return m_baseModel.hasDepthInGui();
    }

    @Override
    public boolean isBuiltin()
    {
        return m_baseModel.isBuiltin();
    }

    @Override
    public Sprite getSprite()
    {
        return m_baseModel.getSprite();
    }

    @Override
    public ModelTransformation getTransformation()
    {
        return m_baseModel.getTransformation();
    }

    @Override
    public ModelItemPropertyOverrideList getItemPropertyOverrides()
    {
        return ModelItemPropertyOverrideList.EMPTY;
    }
}
