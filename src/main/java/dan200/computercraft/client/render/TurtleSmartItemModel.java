/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.client.render;

import com.google.common.base.Objects;
import com.mojang.blaze3d.matrix.MatrixStack;
import dan200.computercraft.api.client.TransformedModel;
import dan200.computercraft.api.turtle.ITurtleUpgrade;
import dan200.computercraft.api.turtle.TurtleSide;
import dan200.computercraft.shared.turtle.items.ItemTurtle;
import dan200.computercraft.shared.util.Holiday;
import dan200.computercraft.shared.util.HolidayUtil;
import net.minecraft.block.BlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.model.*;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Direction;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.vector.TransformationMatrix;
import net.minecraftforge.client.model.data.IModelData;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

public class TurtleSmartItemModel implements IBakedModel
{
    private static final TransformationMatrix identity, flip;

    static
    {
        MatrixStack stack = new MatrixStack();
        stack.scale( 0, -1, 0 );
        stack.translate( 0, 0, 1 );

        identity = TransformationMatrix.identity();
        flip = new TransformationMatrix( stack.last().pose() );
    }

    private static class TurtleModelCombination
    {
        final boolean colour;
        final ITurtleUpgrade leftUpgrade;
        final ITurtleUpgrade rightUpgrade;
        final ResourceLocation overlay;
        final boolean christmas;
        final boolean flip;

        TurtleModelCombination( boolean colour, ITurtleUpgrade leftUpgrade, ITurtleUpgrade rightUpgrade, ResourceLocation overlay, boolean christmas, boolean flip )
        {
            this.colour = colour;
            this.leftUpgrade = leftUpgrade;
            this.rightUpgrade = rightUpgrade;
            this.overlay = overlay;
            this.christmas = christmas;
            this.flip = flip;
        }

        @Override
        public boolean equals( Object other )
        {
            if( other == this ) return true;
            if( !(other instanceof TurtleModelCombination) ) return false;

            TurtleModelCombination otherCombo = (TurtleModelCombination) other;
            return otherCombo.colour == colour &&
                otherCombo.leftUpgrade == leftUpgrade &&
                otherCombo.rightUpgrade == rightUpgrade &&
                Objects.equal( otherCombo.overlay, overlay ) &&
                otherCombo.christmas == christmas &&
                otherCombo.flip == flip;
        }

        @Override
        public int hashCode()
        {
            final int prime = 31;
            int result = 0;
            result = prime * result + (colour ? 1 : 0);
            result = prime * result + (leftUpgrade != null ? leftUpgrade.hashCode() : 0);
            result = prime * result + (rightUpgrade != null ? rightUpgrade.hashCode() : 0);
            result = prime * result + (overlay != null ? overlay.hashCode() : 0);
            result = prime * result + (christmas ? 1 : 0);
            result = prime * result + (flip ? 1 : 0);
            return result;
        }
    }

    private final IBakedModel familyModel;
    private final IBakedModel colourModel;

    private final HashMap<TurtleModelCombination, IBakedModel> cachedModels = new HashMap<>();
    private final ItemOverrideList overrides;

    public TurtleSmartItemModel( IBakedModel familyModel, IBakedModel colourModel )
    {
        this.familyModel = familyModel;
        this.colourModel = colourModel;

        overrides = new ItemOverrideList()
        {
            @Nonnull
            @Override
            public IBakedModel resolve( @Nonnull IBakedModel originalModel, @Nonnull ItemStack stack, @Nullable ClientWorld world, @Nullable LivingEntity entity )
            {
                // Should never happen, but just in case!
                if ( !(stack.getItem() instanceof ItemTurtle) ) return familyModel;

                ItemTurtle turtle = (ItemTurtle) stack.getItem();
                int colour = turtle.getColour( stack );
                ITurtleUpgrade leftUpgrade = turtle.getUpgrade( stack, TurtleSide.LEFT );
                ITurtleUpgrade rightUpgrade = turtle.getUpgrade( stack, TurtleSide.RIGHT );
                ResourceLocation overlay = turtle.getOverlay( stack );
                boolean christmas = HolidayUtil.getCurrentHoliday() == Holiday.CHRISTMAS;
                String label = turtle.getLabel( stack );
                boolean flip = label != null && (label.equals( "Dinnerbone" ) || label.equals( "Grumm" ));
                TurtleModelCombination combo = new TurtleModelCombination( colour != -1, leftUpgrade, rightUpgrade, overlay, christmas, flip );

                IBakedModel model = cachedModels.get( combo );
                if( model == null ) cachedModels.put( combo, model = buildModel( combo ) );
                return model;
            }
        };
    }

    @Nonnull
    @Override
    public ItemOverrideList getOverrides()
    {
        return overrides;
    }

    private IBakedModel buildModel( TurtleModelCombination combo )
    {
        Minecraft mc = Minecraft.getInstance();
        ModelManager modelManager = mc.getItemRenderer().getItemModelShaper().getModelManager();
        ResourceLocation overlayModelLocation = TileEntityTurtleRenderer.getTurtleOverlayModel( combo.overlay, combo.christmas );

        IBakedModel baseModel = combo.colour ? colourModel : familyModel;
        IBakedModel overlayModel = overlayModelLocation != null ? modelManager.getModel( overlayModelLocation ) : null;
        TransformationMatrix transform = combo.flip ? flip : identity;
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
    public ItemCameraTransforms getTransforms()
    {
        return familyModel.getTransforms();
    }

}
