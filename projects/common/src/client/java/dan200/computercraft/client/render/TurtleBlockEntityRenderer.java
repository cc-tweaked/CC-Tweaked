// Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
//
// SPDX-License-Identifier: LicenseRef-CCPL

package dan200.computercraft.client.render;

import com.google.errorprone.annotations.concurrent.LazyInit;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.api.client.StandaloneModel;
import dan200.computercraft.api.turtle.ITurtleAccess;
import dan200.computercraft.api.turtle.TurtleSide;
import dan200.computercraft.client.ClientRegistry;
import dan200.computercraft.client.turtle.TurtleOverlay;
import dan200.computercraft.client.turtle.TurtleOverlayManager;
import dan200.computercraft.client.turtle.TurtleUpgradeModelManager;
import dan200.computercraft.shared.computer.core.ComputerFamily;
import dan200.computercraft.shared.turtle.blocks.TurtleBlockEntity;
import dan200.computercraft.shared.util.Holiday;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.model.ItemTransform;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class TurtleBlockEntityRenderer implements BlockEntityRenderer<TurtleBlockEntity, TurtleBlockEntityRenderer.State> {
    public static final Identifier NORMAL_TURTLE_MODEL = Identifier.fromNamespaceAndPath(ComputerCraftAPI.MOD_ID, "block/turtle_normal");
    public static final Identifier ADVANCED_TURTLE_MODEL = Identifier.fromNamespaceAndPath(ComputerCraftAPI.MOD_ID, "block/turtle_advanced");
    public static final Identifier COLOUR_TURTLE_MODEL = Identifier.fromNamespaceAndPath(ComputerCraftAPI.MOD_ID, "block/turtle_colour");

    private final ItemModelResolver itemModelResolver;

    public TurtleBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        itemModelResolver = context.itemModelResolver();
    }

    public static final class State extends BlockEntityRenderState {
        private @Nullable String label;
        private Vec3 offset = Vec3.ZERO;
        private int colour;
        private float yaw;
        private @LazyInit StandaloneModel model;
        private @Nullable StandaloneModel overlay;
        private @Nullable StandaloneModel elfOverlay;

        private float leftAngle;
        private final ItemStackRenderState leftUpgrade = new ItemStackRenderState();

        private float rightAngle;
        private final ItemStackRenderState rightUpgrade = new ItemStackRenderState();

        private State() {
        }
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(TurtleBlockEntity turtle, State state, float partialTicks, Vec3 camera, ModelFeatureRenderer.@Nullable CrumblingOverlay crumblingOverlay) {
        BlockEntityRenderer.super.extractRenderState(turtle, state, partialTicks, camera, crumblingOverlay);

        var modelManager = Minecraft.getInstance().getModelManager();

        var hit = Minecraft.getInstance().hitResult;
        state.label = hit != null && hit.getType() == HitResult.Type.BLOCK && turtle.getBlockPos().equals(((BlockHitResult) hit).getBlockPos())
            ? turtle.getLabel() : null;
        state.colour = turtle.getColour();
        state.offset = turtle.getRenderOffset(partialTicks);
        state.yaw = turtle.getRenderYaw(partialTicks);

        var modelLocation = state.colour == -1
            ? (turtle.getFamily() == ComputerFamily.NORMAL ? NORMAL_TURTLE_MODEL : ADVANCED_TURTLE_MODEL)
            : COLOUR_TURTLE_MODEL;
        state.model = ClientRegistry.getModel(modelManager, modelLocation);

        var overlay = TurtleOverlayManager.get(modelManager, turtle.getOverlay());
        state.overlay = overlay == null ? null : overlay.model();
        state.elfOverlay = Holiday.getCurrent() == Holiday.CHRISTMAS && (overlay == null || overlay.showElfOverlay())
            ? ClientRegistry.getModel(modelManager, TurtleOverlay.ELF_MODEL)
            : null;

        state.leftAngle = turtle.getToolRenderAngle(TurtleSide.LEFT, partialTicks);
        extractUpgrade(turtle.getAccess(), TurtleSide.LEFT, state.leftUpgrade);

        state.rightAngle = turtle.getToolRenderAngle(TurtleSide.RIGHT, partialTicks);
        extractUpgrade(turtle.getAccess(), TurtleSide.RIGHT, state.rightUpgrade);
    }

    private void extractUpgrade(ITurtleAccess turtle, TurtleSide side, ItemStackRenderState state) {
        state.clear();
        var upgrade = turtle.getUpgradeWithData(side);
        if (upgrade == null) return;

        TurtleUpgradeModelManager.get(Minecraft.getInstance().getModelManager(), upgrade.holder())
            .renderForItem(upgrade, side, state, itemModelResolver, ItemTransform.NO_TRANSFORM, 31);
    }

    @Override
    public void submit(State state, PoseStack transform, SubmitNodeCollector collector, CameraRenderState camera) {
        transform.pushPose();

        // Translate the turtle first, so the label moves with it.
        transform.translate(state.offset);

        if (state.label != null) {
            collector.submitNameTag(
                transform, new Vec3(0.5, 1.2, 0.5), 0, Component.literal(state.label), false, state.lightCoords,
                camera.pos.distanceToSqr(Vec3.atCenterOf(state.blockPos)), // TODO: Should we read camera from the render state instead?
                camera
            );
        }

        // Then apply rotation and flip if needed.
        transform.translate(0.5f, 0.5f, 0.5f);
        transform.mulPose(Axis.YP.rotationDegrees(180.0f - state.yaw));
        transform.translate(-0.5f, -0.5f, -0.5f);

        state.model.submit(transform, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, state.colour == -1 ? null : new int[]{ ARGB.opaque(state.colour) }, state.breakProgress);

        if (state.overlay != null) {
            state.overlay.submit(transform, collector, state.lightCoords, OverlayTexture.NO_OVERLAY);
        }
        if (state.elfOverlay != null) {
            state.elfOverlay.submit(transform, collector, state.lightCoords, OverlayTexture.NO_OVERLAY);
        }

        submitUpgrade(transform, collector, state.lightCoords, state.leftAngle, state.leftUpgrade);
        submitUpgrade(transform, collector, state.lightCoords, state.rightAngle, state.rightUpgrade);

        transform.popPose();
    }

    private void submitUpgrade(PoseStack transform, SubmitNodeCollector collector, int lightmapCoord, float angle, ItemStackRenderState state) {
        if (state.isEmpty()) return;
        transform.pushPose();

        // Swing the tool
        transform.translate(0.0f, 0.5f, 0.5f);
        transform.mulPose(Axis.XN.rotationDegrees(angle));
        transform.translate(0.0f, -0.5f, -0.5f);

        // Then reposition for rendering the item
        transform.translate(0.5f, 0.5f, 0.5f);
        state.submit(transform, collector, lightmapCoord, OverlayTexture.NO_OVERLAY, 0);

        transform.popPose();
    }
}
