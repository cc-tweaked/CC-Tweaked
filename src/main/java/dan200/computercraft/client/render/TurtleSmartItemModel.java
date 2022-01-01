/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Transformation;
import dan200.computercraft.api.client.TransformedModel;
import dan200.computercraft.api.turtle.ITurtleUpgrade;
import dan200.computercraft.api.turtle.TurtleSide;
import dan200.computercraft.shared.turtle.items.ItemTurtle;
import dan200.computercraft.shared.util.Holiday;
import dan200.computercraft.shared.util.HolidayUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.data.IModelData;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

public class TurtleSmartItemModel implements BakedModel
{
    private static final Transformation identity, flip;

    static
    {
        PoseStack stack = new PoseStack();
        stack.scale( 0, -1, 0 );
        stack.translate( 0, 0, 1 );

        identity = Transformation.identity();
        flip = new Transformation( stack.last().pose() );
    }

    private static record TurtleModelCombination(
        boolean colour,
        ITurtleUpgrade leftUpgrade,
        ITurtleUpgrade rightUpgrade,
        ResourceLocation overlay,
        boolean christmas,
        boolean flip
    )
    {
    }

    private final BakedModel familyModel;
    private final BakedModel colourModel;

    private final HashMap<TurtleModelCombination, BakedModel> cachedModels = new HashMap<>();
    private final ItemOverrides overrides;

    public TurtleSmartItemModel( BakedModel familyModel, BakedModel colourModel )
    {
        this.familyModel = familyModel;
        this.colourModel = colourModel;

        overrides = new ItemOverrides()
        {
            @Nonnull
            @Override
            public BakedModel resolve( @Nonnull BakedModel originalModel, @Nonnull ItemStack stack, @Nullable ClientLevel world, @Nullable LivingEntity entity, int random )
            {
                ItemTurtle turtle = (ItemTurtle) stack.getItem();
                int colour = turtle.getColour( stack );
                ITurtleUpgrade leftUpgrade = turtle.getUpgrade( stack, TurtleSide.LEFT );
                ITurtleUpgrade rightUpgrade = turtle.getUpgrade( stack, TurtleSide.RIGHT );
                ResourceLocation overlay = turtle.getOverlay( stack );
                boolean christmas = HolidayUtil.getCurrentHoliday() == Holiday.CHRISTMAS;
                String label = turtle.getLabel( stack );
                boolean flip = label != null && (label.equals( "Dinnerbone" ) || label.equals( "Grumm" ));
                TurtleModelCombination combo = new TurtleModelCombination( colour != -1, leftUpgrade, rightUpgrade, overlay, christmas, flip );

                BakedModel model = cachedModels.get( combo );
                if( model == null ) cachedModels.put( combo, model = buildModel( combo ) );
                return model;
            }
        };
    }

    @Nonnull
    @Override
    public ItemOverrides getOverrides()
    {
        return overrides;
    }

    private BakedModel buildModel( TurtleModelCombination combo )
    {
        Minecraft mc = Minecraft.getInstance();
        ModelManager modelManager = mc.getItemRenderer().getItemModelShaper().getModelManager();
        ModelResourceLocation overlayModelLocation = TileEntityTurtleRenderer.getTurtleOverlayModel( combo.overlay, combo.christmas );

        BakedModel baseModel = combo.colour ? colourModel : familyModel;
        BakedModel overlayModel = overlayModelLocation != null ? modelManager.getModel( overlayModelLocation ) : null;
        Transformation transform = combo.flip ? flip : identity;
        TransformedModel leftModel = combo.leftUpgrade != null ? combo.leftUpgrade.getModel( null, TurtleSide.LEFT ) : null;
        TransformedModel rightModel = combo.rightUpgrade != null ? combo.rightUpgrade.getModel( null, TurtleSide.RIGHT ) : null;
        return new TurtleMultiModel( baseModel, overlayModel, transform, leftModel, rightModel );
    }

    @Nonnull
    @Override
    @Deprecated
    public List<BakedQuad> getQuads( BlockState state, Direction facing, @Nonnull Random rand )
    {
        return familyModel.getQuads( state, facing, rand );
    }

    @Nonnull
    @Override
    @Deprecated
    public List<BakedQuad> getQuads( BlockState state, Direction facing, @Nonnull Random rand, @Nonnull IModelData data )
    {
        return familyModel.getQuads( state, facing, rand, data );
    }

    @Override
    public boolean useAmbientOcclusion()
    {
        return familyModel.useAmbientOcclusion();
    }

    @Override
    public boolean isGui3d()
    {
        return familyModel.isGui3d();
    }

    @Override
    public boolean isCustomRenderer()
    {
        return familyModel.isCustomRenderer();
    }

    @Override
    public boolean usesBlockLight()
    {
        return familyModel.usesBlockLight();
    }

    @Nonnull
    @Override
    @Deprecated
    public TextureAtlasSprite getParticleIcon()
    {
        return familyModel.getParticleIcon();
    }

    @Nonnull
    @Override
    @Deprecated
    public ItemTransforms getTransforms()
    {
        return familyModel.getTransforms();
    }

}
