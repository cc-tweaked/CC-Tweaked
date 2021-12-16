/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.client.render;

import com.mojang.math.Transformation;
import dan200.computercraft.api.client.TransformedModel;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nonnull;
import java.util.*;

@Environment( EnvType.CLIENT )
public class TurtleMultiModel implements BakedModel
{
    private final BakedModel baseModel;
    private final BakedModel overlayModel;
    private final Transformation generalTransform;
    private final TransformedModel leftUpgradeModel;
    private final TransformedModel rightUpgradeModel;
    private List<BakedQuad> generalQuads = null;
    private final Map<Direction, List<BakedQuad>> faceQuads = new EnumMap<>( Direction.class );

    public TurtleMultiModel( BakedModel baseModel, BakedModel overlayModel, Transformation generalTransform, TransformedModel leftUpgradeModel,
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
            if( !faceQuads.containsKey( side ) )
            {
                faceQuads.put( side, buildQuads( state, side, rand ) );
            }
            return faceQuads.get( side );
        }
        else
        {
            if( generalQuads == null )
            {
                generalQuads = buildQuads( state, side, rand );
            }
            return generalQuads;
        }
    }

    private List<BakedQuad> buildQuads( BlockState state, Direction side, Random rand )
    {
        ArrayList<BakedQuad> quads = new ArrayList<>();


        ModelTransformer.transformQuadsTo( quads, baseModel.getQuads( state, side, rand ), generalTransform.getMatrix() );
        if( overlayModel != null )
        {
            ModelTransformer.transformQuadsTo( quads, overlayModel.getQuads( state, side, rand ), generalTransform.getMatrix() );
        }
        if( leftUpgradeModel != null )
        {
            Transformation upgradeTransform = generalTransform.compose( leftUpgradeModel.getMatrix() );
            ModelTransformer.transformQuadsTo( quads, leftUpgradeModel.getModel()
                    .getQuads( state, side, rand ),
                upgradeTransform.getMatrix() );
        }
        if( rightUpgradeModel != null )
        {
            Transformation upgradeTransform = generalTransform.compose( rightUpgradeModel.getMatrix() );
            ModelTransformer.transformQuadsTo( quads, rightUpgradeModel.getModel()
                    .getQuads( state, side, rand ),
                upgradeTransform.getMatrix() );
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
    public boolean usesBlockLight()
    {
        return baseModel.usesBlockLight();
    }

    @Override
    public boolean isCustomRenderer()
    {
        return baseModel.isCustomRenderer();
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
    public ItemTransforms getTransforms()
    {
        return baseModel.getTransforms();
    }

    @Nonnull
    @Override
    public ItemOverrides getOverrides()
    {
        return ItemOverrides.EMPTY;
    }
}
