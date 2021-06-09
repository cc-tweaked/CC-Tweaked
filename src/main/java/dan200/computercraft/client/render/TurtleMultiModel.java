/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. Do not distribute without permission.
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

@Environment( EnvType.CLIENT )
public class TurtleMultiModel implements BakedModel
{
    private final BakedModel baseModel;
    private final BakedModel overlayModel;
    private final AffineTransformation generalTransform;
    private final TransformedModel leftUpgradeModel;
    private final TransformedModel rightUpgradeModel;
    private List<BakedQuad> generalQuads = null;
    private Map<Direction, List<BakedQuad>> faceQuads = new EnumMap<>( Direction.class );

    public TurtleMultiModel( BakedModel baseModel, BakedModel overlayModel, AffineTransformation generalTransform, TransformedModel leftUpgradeModel,
                             TransformedModel rightUpgradeModel )
    {
        // Get the models
        this.baseModel = baseModel;
        this.overlayModel = overlayModel;
        this.leftUpgradeModel = leftUpgradeModel;
        this.rightUpgradeModel = rightUpgradeModel;
        this.generalTransform = generalTransform;
    }

    @Nonnull
    @Override
    public List<BakedQuad> getQuads( BlockState state, Direction side, @Nonnull Random rand )
    {
        if( side != null )
        {
            if( !this.faceQuads.containsKey( side ) )
            {
                this.faceQuads.put( side, this.buildQuads( state, side, rand ) );
            }
            return this.faceQuads.get( side );
        }
        else
        {
            if( this.generalQuads == null )
            {
                this.generalQuads = this.buildQuads( state, side, rand );
            }
            return this.generalQuads;
        }
    }

    private List<BakedQuad> buildQuads( BlockState state, Direction side, Random rand )
    {
        ArrayList<BakedQuad> quads = new ArrayList<>();


        ModelTransformer.transformQuadsTo( quads, this.baseModel.getQuads( state, side, rand ), this.generalTransform.getMatrix() );
        if( this.overlayModel != null )
        {
            ModelTransformer.transformQuadsTo( quads, this.overlayModel.getQuads( state, side, rand ), this.generalTransform.getMatrix() );
        }
        if( this.leftUpgradeModel != null )
        {
            AffineTransformation upgradeTransform = this.generalTransform.multiply( this.leftUpgradeModel.getMatrix() );
            ModelTransformer.transformQuadsTo( quads, this.leftUpgradeModel.getModel()
                    .getQuads( state, side, rand ),
                upgradeTransform.getMatrix() );
        }
        if( this.rightUpgradeModel != null )
        {
            AffineTransformation upgradeTransform = this.generalTransform.multiply( this.rightUpgradeModel.getMatrix() );
            ModelTransformer.transformQuadsTo( quads, this.rightUpgradeModel.getModel()
                    .getQuads( state, side, rand ),
                upgradeTransform.getMatrix() );
        }
        quads.trimToSize();
        return quads;
    }

    @Override
    public boolean useAmbientOcclusion()
    {
        return this.baseModel.useAmbientOcclusion();
    }

    @Override
    public boolean hasDepth()
    {
        return this.baseModel.hasDepth();
    }

    @Override
    public boolean isSideLit()
    {
        return this.baseModel.isSideLit();
    }

    @Override
    public boolean isBuiltin()
    {
        return this.baseModel.isBuiltin();
    }

    @Nonnull
    @Override
    @Deprecated
    public Sprite getSprite()
    {
        return this.baseModel.getSprite();
    }

    @Nonnull
    @Override
    @Deprecated
    public net.minecraft.client.render.model.json.ModelTransformation getTransformation()
    {
        return this.baseModel.getTransformation();
    }

    @Nonnull
    @Override
    public ModelOverrideList getOverrides()
    {
        return ModelOverrideList.EMPTY;
    }
}
