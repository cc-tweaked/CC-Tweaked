/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.client.render;

import dan200.computercraft.api.client.TransformedModel;
import net.minecraft.block.BlockState;
import net.minecraft.client.renderer.model.BakedQuad;
import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.client.renderer.model.ItemOverrideList;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.Direction;
import net.minecraft.util.math.vector.TransformationMatrix;
import net.minecraftforge.client.model.data.EmptyModelData;
import net.minecraftforge.client.model.data.IModelData;
import net.minecraftforge.client.model.pipeline.BakedQuadBuilder;
import net.minecraftforge.client.model.pipeline.TRSRTransformer;

import javax.annotation.Nonnull;
import java.util.*;

public class TurtleMultiModel implements IBakedModel
{
    private final IBakedModel baseModel;
    private final IBakedModel overlayModel;
    private final TransformationMatrix generalTransform;
    private final TransformedModel leftUpgradeModel;
    private final TransformedModel rightUpgradeModel;
    private List<BakedQuad> generalQuads = null;
    private final Map<Direction, List<BakedQuad>> faceQuads = new EnumMap<>( Direction.class );

    public TurtleMultiModel( IBakedModel baseModel, IBakedModel overlayModel, TransformationMatrix generalTransform, TransformedModel leftUpgradeModel, TransformedModel rightUpgradeModel )
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
    @Deprecated
    public List<BakedQuad> getQuads( BlockState state, Direction side, @Nonnull Random rand )
    {
        return getQuads( state, side, rand, EmptyModelData.INSTANCE );
    }

    @Nonnull
    @Override
    public List<BakedQuad> getQuads( BlockState state, Direction side, @Nonnull Random rand, @Nonnull IModelData data )
    {
        if( side != null )
        {
            if( !faceQuads.containsKey( side ) ) faceQuads.put( side, buildQuads( state, side, rand ) );
            return faceQuads.get( side );
        }
        else
        {
            if( generalQuads == null ) generalQuads = buildQuads( state, side, rand );
            return generalQuads;
        }
    }

    private List<BakedQuad> buildQuads( BlockState state, Direction side, Random rand )
    {
        ArrayList<BakedQuad> quads = new ArrayList<>();


        transformQuadsTo( quads, baseModel.getQuads( state, side, rand, EmptyModelData.INSTANCE ), generalTransform );
        if( overlayModel != null )
        {
            transformQuadsTo( quads, overlayModel.getQuads( state, side, rand, EmptyModelData.INSTANCE ), generalTransform );
        }
        if( leftUpgradeModel != null )
        {
            TransformationMatrix upgradeTransform = generalTransform.compose( leftUpgradeModel.getMatrix() );
            transformQuadsTo( quads, leftUpgradeModel.getModel().getQuads( state, side, rand, EmptyModelData.INSTANCE ), upgradeTransform );
        }
        if( rightUpgradeModel != null )
        {
            TransformationMatrix upgradeTransform = generalTransform.compose( rightUpgradeModel.getMatrix() );
            transformQuadsTo( quads, rightUpgradeModel.getModel().getQuads( state, side, rand, EmptyModelData.INSTANCE ), upgradeTransform );
        }
        quads.trimToSize();
        return quads;
    }

    @Override
    public boolean useAmbientOcclusion()
    {
        return baseModel.useAmbientOcclusion();
    }

    @Override
    public boolean isGui3d()
    {
        return baseModel.isGui3d();
    }

    @Override
    public boolean isCustomRenderer()
    {
        return baseModel.isCustomRenderer();
    }

    @Override
    public boolean usesBlockLight()
    {
        return baseModel.usesBlockLight();
    }

    @Nonnull
    @Override
    @Deprecated
    public TextureAtlasSprite getParticleIcon()
    {
        return baseModel.getParticleIcon();
    }

    @Nonnull
    @Override
    @Deprecated
    public net.minecraft.client.renderer.model.ItemCameraTransforms getTransforms()
    {
        return baseModel.getTransforms();
    }

    @Nonnull
    @Override
    public ItemOverrideList getOverrides()
    {
        return ItemOverrideList.EMPTY;
    }

    private void transformQuadsTo( List<BakedQuad> output, List<BakedQuad> quads, TransformationMatrix transform )
    {
        for( BakedQuad quad : quads )
        {
            BakedQuadBuilder builder = new BakedQuadBuilder();
            TRSRTransformer transformer = new TRSRTransformer( builder, transform );
            quad.pipe( transformer );
            output.add( builder.build() );
        }
    }
}
