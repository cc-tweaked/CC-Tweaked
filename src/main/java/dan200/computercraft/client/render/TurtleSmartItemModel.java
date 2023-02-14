/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Transformation;
import dan200.computercraft.api.turtle.ITurtleUpgrade;
import dan200.computercraft.api.turtle.TurtleSide;
import dan200.computercraft.client.turtle.TurtleUpgradeModellers;
import dan200.computercraft.shared.turtle.items.ItemTurtle;
import dan200.computercraft.shared.util.Holiday;
import dan200.computercraft.shared.util.HolidayUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.model.BakedModelWrapper;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TurtleSmartItemModel extends BakedModelWrapper<BakedModel>
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

    private record TurtleModelCombination(
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

    private final Map<TurtleModelCombination, List<BakedModel>> cachedModels = new HashMap<>();

    public TurtleSmartItemModel( BakedModel familyModel, BakedModel colourModel )
    {
        super( familyModel );
        this.familyModel = familyModel;
        this.colourModel = colourModel;
    }

    @Nonnull
    @Override
    public BakedModel applyTransform( @Nonnull ItemTransforms.TransformType cameraTransformType, @Nonnull PoseStack poseStack, boolean applyLeftHandTransform )
    {
        originalModel.applyTransform( cameraTransformType, poseStack, applyLeftHandTransform );
        return this;
    }

    @Nonnull
    @Override
    public List<BakedModel> getRenderPasses( ItemStack stack, boolean fabulous )
    {
        if ( !(stack.getItem() instanceof ItemTurtle) ) return familyModel.getRenderPasses( stack, fabulous );
        ItemTurtle turtle = (ItemTurtle) stack.getItem();

        int colour = turtle.getColour( stack );
        ITurtleUpgrade leftUpgrade = turtle.getUpgrade( stack, TurtleSide.LEFT );
        ITurtleUpgrade rightUpgrade = turtle.getUpgrade( stack, TurtleSide.RIGHT );
        ResourceLocation overlay = turtle.getOverlay( stack );
        boolean christmas = HolidayUtil.getCurrentHoliday() == Holiday.CHRISTMAS;
        String label = turtle.getLabel( stack );
        boolean flip = label != null && (label.equals( "Dinnerbone" ) || label.equals( "Grumm" ));

        TurtleModelCombination combo = new TurtleModelCombination( colour != -1, leftUpgrade, rightUpgrade, overlay, christmas, flip );
        return cachedModels.computeIfAbsent( combo, this::buildModel );
    }

    private List<BakedModel> buildModel( TurtleModelCombination combo )
    {
        Minecraft mc = Minecraft.getInstance();
        ModelManager modelManager = mc.getItemRenderer().getItemModelShaper().getModelManager();

        var transformation = combo.flip ? flip : identity;
        ArrayList<BakedModel> parts = new ArrayList<>( 4 );
        parts.add( new TransformedBakedModel( combo.colour() ? colourModel : familyModel, transformation ) );

        ResourceLocation overlayModelLocation = TileEntityTurtleRenderer.getTurtleOverlayModel( combo.overlay(), combo.christmas() );
        if( overlayModelLocation != null )
        {
            parts.add( new TransformedBakedModel( modelManager.getModel( overlayModelLocation ), transformation ) );
        }
        if( combo.leftUpgrade() != null )
        {
            parts.add( new TransformedBakedModel( TurtleUpgradeModellers.getModel( combo.leftUpgrade(), null, TurtleSide.LEFT ) ).composeWith( transformation ) );
        }
        if( combo.rightUpgrade() != null )
        {
            parts.add( new TransformedBakedModel( TurtleUpgradeModellers.getModel( combo.rightUpgrade(), null, TurtleSide.RIGHT ) ).composeWith( transformation ) );
        }

        return parts;
    }
}
