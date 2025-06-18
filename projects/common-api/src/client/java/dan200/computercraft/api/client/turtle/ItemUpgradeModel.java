// SPDX-FileCopyrightText: 2025 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.api.client.turtle;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.mojang.math.Transformation;
import com.mojang.serialization.MapCodec;
import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.api.turtle.ITurtleAccess;
import dan200.computercraft.api.turtle.ITurtleUpgrade;
import dan200.computercraft.api.turtle.TurtleSide;
import dan200.computercraft.api.upgrades.UpgradeData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.model.ItemTransform;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

import java.util.Set;

/**
 * A sic {@link TurtleUpgradeModel} that renders the upgrade's {@linkplain ITurtleUpgrade#getUpgradeItem(DataComponentPatch)
 * upgrade item}.
 * <p>
 * This uses appropriate transformations for "flat" items, namely those extending the {@literal minecraft:item/generated}
 * model type. It will not appear correct for 3D models with additional depth, such as blocks.
 */
public final class ItemUpgradeModel implements TurtleUpgradeModel {
    private static final TurtleUpgradeModel.Unbaked UNBAKED = new Unbaked();
    private static final TurtleUpgradeModel INSTANCE = new ItemUpgradeModel();

    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(ComputerCraftAPI.MOD_ID, "item");
    public static final MapCodec<TurtleUpgradeModel.Unbaked> CODEC = MapCodec.unit(UNBAKED);

    private static final TransformedRenderer LEFT = computeRenderer(TurtleSide.LEFT);
    private static final TransformedRenderer RIGHT = computeRenderer(TurtleSide.RIGHT);

    private ItemUpgradeModel() {
    }

    /**
     * Get the unbaked {@link ItemUpgradeModel}.
     *
     * @return The unbaked item upgrade model.
     */
    public static TurtleUpgradeModel.Unbaked unbaked() {
        return UNBAKED;
    }

    @Override
    public void renderForItem(UpgradeData<ITurtleUpgrade> upgrade, TurtleSide side, ItemStackRenderState renderer, ItemModelResolver resolver, ItemTransform transform, int seed) {
        renderer.appendModelIdentityElement(this);

        var childState = new ItemStackRenderState();
        resolver.updateForTopItem(childState, upgrade.getUpgradeItem(), ItemDisplayContext.NONE, null, null, seed);
        if (!childState.isEmpty()) {
            renderer.appendModelIdentityElement(childState.getModelIdentity());
            renderer.appendModelIdentityElement(transform);

            var layer = renderer.newLayer();
            layer.setTransform(transform);
            layer.setupSpecialModel(getRenderer(side), childState);
        }
    }

    @Override
    public void renderForLevel(UpgradeData<ITurtleUpgrade> upgrade, TurtleSide side, ITurtleAccess turtle, PoseStack transform, MultiBufferSource buffers, int light, int overlay) {
        transform.mulPose(getRenderer(side).transform().getMatrix());
        transform.mulPose(Axis.YP.rotation(Mth.PI));
        Minecraft.getInstance().getItemRenderer().renderStatic(
            upgrade.getUpgradeItem(), ItemDisplayContext.FIXED, light, overlay, transform, buffers, turtle.getLevel(), 0
        );
    }

    private static final class Unbaked implements TurtleUpgradeModel.Unbaked {
        @Override
        public MapCodec<? extends TurtleUpgradeModel.Unbaked> type() {
            return CODEC;
        }

        @Override
        public TurtleUpgradeModel bake(ModelBaker baker) {
            return INSTANCE;
        }

        @Override
        public void resolveDependencies(Resolver resolver) {
        }
    }

    private static TransformedRenderer computeRenderer(TurtleSide side) {
        var pose = new Matrix4f();
        pose.translate(0.5f, 0.5f, 0.5f);
        pose.rotate(Axis.YN.rotationDegrees(90f));
        pose.rotate(Axis.ZP.rotationDegrees(90f));
        pose.translate(0.0f, 0.0f, side == TurtleSide.RIGHT ? -0.4065f : 0.4065f);
        return new TransformedRenderer(new Transformation(pose));
    }

    private static TransformedRenderer getRenderer(TurtleSide side) {
        return switch (side) {
            case LEFT -> LEFT;
            case RIGHT -> RIGHT;
        };
    }

    private record TransformedRenderer(Transformation transform) implements SpecialModelRenderer<ItemStackRenderState> {
        @Override
        public void render(
            @Nullable ItemStackRenderState state, ItemDisplayContext itemDisplayContext, PoseStack poseStack,
            MultiBufferSource multiBufferSource, int overlay, int light, boolean bl
        ) {
            if (state == null) return;
            poseStack.pushPose();
            poseStack.mulPose(transform.getMatrix());
            state.render(poseStack, multiBufferSource, overlay, light);
            poseStack.popPose();
        }

        @Override
        public void getExtents(Set<Vector3f> set) {
        }

        @Override
        public @Nullable ItemStackRenderState extractArgument(ItemStack itemStack) {
            return null;
        }
    }
}
