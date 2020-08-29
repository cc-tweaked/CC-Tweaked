/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.client.render;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.vecmath.Matrix4f;

import com.google.common.base.Objects;
import dan200.computercraft.api.turtle.ITurtleUpgrade;
import dan200.computercraft.api.turtle.TurtleSide;
import dan200.computercraft.shared.turtle.items.ItemTurtle;
import dan200.computercraft.shared.util.Holiday;
import dan200.computercraft.shared.util.HolidayUtil;
import org.apache.commons.lang3.tuple.Pair;

import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.BakedModelManager;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.render.model.ModelLoader;
import net.minecraft.client.render.model.json.ModelOverrideList;
import net.minecraft.client.render.model.json.ModelTransformation;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.ModelIdentifier;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

public class TurtleSmartItemModel implements BakedModel {
    private static final Matrix4f s_identity, s_flip;

    static {
        s_identity = new Matrix4f();
        s_identity.setIdentity();

        s_flip = new Matrix4f();
        s_flip.setIdentity();
        s_flip.m11 = -1; // Flip on the y axis
        s_flip.m13 = 1; // Models go from (0,0,0) to (1,1,1), so push back up.
    }

    private final BakedModel familyModel;
    private final BakedModel colourModel;
    private HashMap<TurtleModelCombination, BakedModel> m_cachedModels;
    private ModelOverrideList m_overrides;
    public TurtleSmartItemModel(ModelLoader loader, BakedModel familyModel, BakedModel colourModel) {
        this.familyModel = familyModel;
        this.colourModel = colourModel;

        this.m_cachedModels = new HashMap<>();
        this.m_overrides = new ModelOverrideList(loader, null, null, Collections.emptyList()) {
            @Nonnull
            @Override
            public BakedModel apply(@Nonnull BakedModel originalModel, @Nonnull ItemStack stack, @Nullable World world, @Nullable LivingEntity entity) {
                ItemTurtle turtle = (ItemTurtle) stack.getItem();
                int colour = turtle.getColour(stack);
                ITurtleUpgrade leftUpgrade = turtle.getUpgrade(stack, TurtleSide.Left);
                ITurtleUpgrade rightUpgrade = turtle.getUpgrade(stack, TurtleSide.Right);
                Identifier overlay = turtle.getOverlay(stack);
                boolean christmas = HolidayUtil.getCurrentHoliday() == Holiday.Christmas;
                String label = turtle.getLabel(stack);
                boolean flip = label != null && (label.equals("Dinnerbone") || label.equals("Grumm"));
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
        Matrix4f transform = combo.m_flip ? s_flip : s_identity;
        Pair<BakedModel, Matrix4f> leftModel = combo.m_leftUpgrade != null ? combo.m_leftUpgrade.getModel(null, TurtleSide.Left) : null;
        Pair<BakedModel, Matrix4f> rightModel = combo.m_rightUpgrade != null ? combo.m_rightUpgrade.getModel(null, TurtleSide.Right) : null;
        if (leftModel != null && rightModel != null) {
            return new TurtleMultiModel(baseModel,
                                        overlayModel,
                                        transform,
                                        leftModel.getLeft(),
                                        leftModel.getRight(),
                                        rightModel.getLeft(),
                                        rightModel.getRight());
        } else if (leftModel != null) {
            return new TurtleMultiModel(baseModel, overlayModel, transform, leftModel.getLeft(), leftModel.getRight(), null, null);
        } else if (rightModel != null) {
            return new TurtleMultiModel(baseModel, overlayModel, transform, null, null, rightModel.getLeft(), rightModel.getRight());
        } else {
            return new TurtleMultiModel(baseModel, overlayModel, transform, null, null, null, null);
        }
    }

    @Nonnull
    @Override
    @Deprecated
    public List<BakedQuad> getQuads(BlockState state, Direction facing, Random rand) {
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
    public boolean isBuiltin() {
        return this.familyModel.isBuiltin();
    }

    @Override
    public Sprite getSprite() {
        return null;
    }

    @Override
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
