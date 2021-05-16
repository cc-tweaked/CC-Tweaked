/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.client.render;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.google.common.base.Objects;
import dan200.computercraft.api.client.TransformedModel;
import dan200.computercraft.api.turtle.ITurtleUpgrade;
import dan200.computercraft.api.turtle.TurtleSide;
import dan200.computercraft.shared.turtle.items.ItemTurtle;
import dan200.computercraft.shared.turtle.upgrades.TurtleTool;
import dan200.computercraft.shared.util.Holiday;
import dan200.computercraft.shared.util.HolidayUtil;

import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.BakedModelManager;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.render.model.json.ModelOverrideList;
import net.minecraft.client.render.model.json.ModelTransformation;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.ModelIdentifier;
import net.minecraft.client.util.math.AffineTransformation;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Direction;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment (EnvType.CLIENT)
public class TurtleSmartItemModel implements BakedModel {
    private static final AffineTransformation identity, flip;

    static {
        MatrixStack stack = new MatrixStack();
        stack.scale(0, -1, 0);
        stack.translate(0, 0, 1);

        identity = AffineTransformation.identity();
        flip = new AffineTransformation(stack.peek()
                                             .getModel());
    }

    private final BakedModel familyModel;
    private final BakedModel colourModel;
    private final HashMap<TurtleModelCombination, BakedModel> m_cachedModels = new HashMap<>();
    private final ModelOverrideList m_overrides;
    public TurtleSmartItemModel(BakedModel familyModel, BakedModel colourModel) {
        this.familyModel = familyModel;
        this.colourModel = colourModel;

        // this actually works I think, trust me
        this.m_overrides = new ModelOverrideList(null, null, null, Collections.emptyList()) {
            @Nonnull
            @Override
            public BakedModel apply(@Nonnull BakedModel originalModel, @Nonnull ItemStack stack, @Nullable ClientWorld world,
                                    @Nullable LivingEntity entity) {
                ItemTurtle turtle = (ItemTurtle) stack.getItem();
                int colour = turtle.getColour(stack);
                ITurtleUpgrade leftUpgrade = turtle.getUpgrade(stack, TurtleSide.LEFT);
                ITurtleUpgrade rightUpgrade = turtle.getUpgrade(stack, TurtleSide.RIGHT);
                Identifier overlay = turtle.getOverlay(stack);
                boolean christmas = HolidayUtil.getCurrentHoliday() == Holiday.CHRISTMAS;
                String label = turtle.getLabel(stack);
                // TODO make upside down turtle items render properly (currently inivisible)
                //boolean flip = label != null && (label.equals("Dinnerbone") || label.equals("Grumm"));
                boolean flip = false;
                TurtleModelCombination combo = new TurtleModelCombination(colour != -1, leftUpgrade, rightUpgrade, overlay, christmas, flip);

                BakedModel model = TurtleSmartItemModel.this.m_cachedModels.get(combo);
                if (model == null) {
                    TurtleSmartItemModel.this.m_cachedModels.put(combo, model = TurtleSmartItemModel.this.buildModel(combo));
                }
                return model;
            }
        };
    }

    private BakedModel buildModel(TurtleModelCombination combo) {
        MinecraftClient mc = MinecraftClient.getInstance();
        BakedModelManager modelManager = mc.getItemRenderer()
                                           .getModels()
                                           .getModelManager();
        ModelIdentifier overlayModelLocation = TileEntityTurtleRenderer.getTurtleOverlayModel(combo.m_overlay, combo.m_christmas);

        BakedModel baseModel = combo.m_colour ? this.colourModel : this.familyModel;
        BakedModel overlayModel = overlayModelLocation != null ? modelManager.getModel(overlayModelLocation) : null;
        AffineTransformation transform = combo.m_flip ? flip : identity;
        TransformedModel leftModel = combo.m_leftUpgrade != null ? combo.m_leftUpgrade.getModel(null, TurtleSide.LEFT) : null;
        TransformedModel rightModel = combo.m_rightUpgrade != null ? combo.m_rightUpgrade.getModel(null, TurtleSide.RIGHT) : null;
        return new TurtleMultiModel(baseModel, overlayModel, transform, leftModel, rightModel);
    }

    @Nonnull
    @Override
    @Deprecated
    public List<BakedQuad> getQuads(BlockState state, Direction facing, @Nonnull Random rand) {
        return this.familyModel.getQuads(state, facing, rand);
    }

    @Override
    public boolean useAmbientOcclusion() {
        return this.familyModel.useAmbientOcclusion();
    }

    @Override
    public boolean hasDepth() {
        return this.familyModel.hasDepth();
    }

    @Override
    public boolean isSideLit() {
        return this.familyModel.isSideLit();
    }

    @Override
    public boolean isBuiltin() {
        return this.familyModel.isBuiltin();
    }

    @Nonnull
    @Override
    @Deprecated
    public Sprite getSprite() {
        return this.familyModel.getSprite();
    }

    @Nonnull
    @Override
    @Deprecated
    public ModelTransformation getTransformation() {
        return this.familyModel.getTransformation();
    }

    @Nonnull
    @Override
    public ModelOverrideList getOverrides() {
        return this.m_overrides;
    }

    private static class TurtleModelCombination {
        final boolean m_colour;
        final ITurtleUpgrade m_leftUpgrade;
        final ITurtleUpgrade m_rightUpgrade;
        final Identifier m_overlay;
        final boolean m_christmas;
        final boolean m_flip;

        TurtleModelCombination(boolean colour, ITurtleUpgrade leftUpgrade, ITurtleUpgrade rightUpgrade, Identifier overlay, boolean christmas,
                               boolean flip) {
            this.m_colour = colour;
            this.m_leftUpgrade = leftUpgrade;
            this.m_rightUpgrade = rightUpgrade;
            this.m_overlay = overlay;
            this.m_christmas = christmas;
            this.m_flip = flip;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 0;
            result = prime * result + (this.m_colour ? 1 : 0);
            result = prime * result + (this.m_leftUpgrade != null ? this.m_leftUpgrade.hashCode() : 0);
            result = prime * result + (this.m_rightUpgrade != null ? this.m_rightUpgrade.hashCode() : 0);
            result = prime * result + (this.m_overlay != null ? this.m_overlay.hashCode() : 0);
            result = prime * result + (this.m_christmas ? 1 : 0);
            result = prime * result + (this.m_flip ? 1 : 0);
            return result;
        }

        @Override
        public boolean equals(Object other) {
            if (other == this) {
                return true;
            }
            if (!(other instanceof TurtleModelCombination)) {
                return false;
            }

            TurtleModelCombination otherCombo = (TurtleModelCombination) other;
            return otherCombo.m_colour == this.m_colour && otherCombo.m_leftUpgrade == this.m_leftUpgrade && otherCombo.m_rightUpgrade == this.m_rightUpgrade && Objects.equal(
                otherCombo.m_overlay, this.m_overlay) && otherCombo.m_christmas == this.m_christmas && otherCombo.m_flip == this.m_flip;
        }
    }

}
