/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.client.render;

import dan200.computercraft.api.client.TransformedModel;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.render.model.json.ModelOverrideList;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.math.AffineTransformation;
import net.minecraft.util.math.Direction;

import javax.annotation.Nonnull;
import java.util.*;

@Environment(EnvType.CLIENT)
public class TurtleMultiModel implements BakedModel
{
    private final BakedModel m_baseModel;
    private final BakedModel m_overlayModel;
    private final AffineTransformation m_generalTransform;
    private final TransformedModel m_leftUpgradeModel;
    private final TransformedModel m_rightUpgradeModel;
    private List<BakedQuad> m_generalQuads = null;
    private Map<Direction, List<BakedQuad>> m_faceQuads = new EnumMap<>( Direction.class );

    public TurtleMultiModel( BakedModel baseModel, BakedModel overlayModel, AffineTransformation generalTransform, TransformedModel leftUpgradeModel, TransformedModel rightUpgradeModel )
    {
        // Get the models
        m_baseModel = baseModel;
        m_overlayModel = overlayModel;
        m_leftUpgradeModel = leftUpgradeModel;
        m_rightUpgradeModel = rightUpgradeModel;
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


        ModelTransformer.transformQuadsTo( quads, m_baseModel.getQuads( state, side, rand ), m_generalTransform.getMatrix() );
        if( m_overlayModel != null )
        {
            ModelTransformer.transformQuadsTo( quads, m_overlayModel.getQuads( state, side, rand ), m_generalTransform.getMatrix() );
        }
        if( m_leftUpgradeModel != null )
        {
            AffineTransformation upgradeTransform = m_generalTransform.multiply( m_leftUpgradeModel.getMatrix() );
            ModelTransformer.transformQuadsTo( quads, m_leftUpgradeModel.getModel().getQuads( state, side, rand ), upgradeTransform.getMatrix() );
        }
        if( m_rightUpgradeModel != null )
        {
            AffineTransformation upgradeTransform = m_generalTransform.multiply( m_rightUpgradeModel.getMatrix() );
            ModelTransformer.transformQuadsTo( quads, m_rightUpgradeModel.getModel().getQuads( state, side, rand ), upgradeTransform.getMatrix() );
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
    public boolean hasDepth()
    {
        return m_baseModel.hasDepth();
    }

    @Override
    public boolean isBuiltin()
    {
        return m_baseModel.isBuiltin();
    }

    @Override
    public boolean isSideLit()
    {
        return m_baseModel.isSideLit();
    }

    @Nonnull
    @Override
    @Deprecated
    public Sprite getSprite()
    {
        return m_baseModel.getSprite();
    }

    @Nonnull
    @Override
    @Deprecated
    public net.minecraft.client.render.model.json.ModelTransformation getTransformation()
    {
        return m_baseModel.getTransformation();
    }

    @Nonnull
    @Override
    public ModelOverrideList getOverrides()
    {
        return ModelOverrideList.EMPTY;
    }
}
