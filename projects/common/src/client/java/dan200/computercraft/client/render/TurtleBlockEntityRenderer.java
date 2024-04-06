// Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
//
// SPDX-License-Identifier: LicenseRef-CCPL

package dan200.computercraft.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.mojang.math.Transformation;
import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.api.turtle.TurtleSide;
import dan200.computercraft.client.platform.ClientPlatformHelper;
import dan200.computercraft.client.turtle.TurtleUpgradeModellers;
import dan200.computercraft.shared.turtle.blocks.TurtleBlockEntity;
import dan200.computercraft.shared.util.Holiday;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import javax.annotation.Nullable;

public class TurtleBlockEntityRenderer implements BlockEntityRenderer<TurtleBlockEntity> {
    private static final ResourceLocation COLOUR_TURTLE_MODEL = new ResourceLocation(ComputerCraftAPI.MOD_ID, "block/turtle_colour");
    private static final ResourceLocation ELF_OVERLAY_MODEL = new ResourceLocation(ComputerCraftAPI.MOD_ID, "block/turtle_elf_overlay");

    private final BlockEntityRenderDispatcher renderer;
    private final Font font;

    public TurtleBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        renderer = context.getBlockEntityRenderDispatcher();
        font = context.getFont();
    }

    public static @Nullable ResourceLocation getTurtleOverlayModel(@Nullable ResourceLocation overlay, boolean christmas) {
        if (overlay != null) return overlay;
        if (christmas) return ELF_OVERLAY_MODEL;
        return null;
    }

    @Override
    public void render(TurtleBlockEntity turtle, float partialTicks, PoseStack transform, MultiBufferSource buffers, int lightmapCoord, int overlayLight) {
        transform.pushPose();

        // Translate the turtle first, so the label moves with it.
        var offset = turtle.getRenderOffset(partialTicks);
        transform.translate(offset.x, offset.y, offset.z);

        // Render the label
        var label = turtle.getLabel();
        var hit = renderer.cameraHitResult;
        if (label != null && hit != null && hit.getType() == HitResult.Type.BLOCK && turtle.getBlockPos().equals(((BlockHitResult) hit).getBlockPos())) {
            var mc = Minecraft.getInstance();
            var font = this.font;

            transform.pushPose();
            transform.translate(0.5, 1.2, 0.5);
            transform.mulPose(mc.getEntityRenderDispatcher().cameraOrientation());
            transform.scale(-0.025f, -0.025f, 0.025f);

            var matrix = transform.last().pose();
            var opacity = (int) (mc.options.getBackgroundOpacity(0.25f) * 255) << 24;
            var width = -font.width(label) / 2.0f;
            font.drawInBatch(label, width, (float) 0, 0x20ffffff, false, matrix, buffers, Font.DisplayMode.SEE_THROUGH, opacity, lightmapCoord);
            font.drawInBatch(label, width, (float) 0, 0xffffffff, false, matrix, buffers, Font.DisplayMode.NORMAL, 0, lightmapCoord);

            transform.popPose();
        }

        // Then apply rotation and flip if needed.
        transform.translate(0.5f, 0.5f, 0.5f);
        var yaw = turtle.getRenderYaw(partialTicks);
        transform.mulPose(Axis.YP.rotationDegrees(180.0f - yaw));
        if (label != null && (label.equals("Dinnerbone") || label.equals("Grumm"))) {
            transform.scale(1.0f, -1.0f, 1.0f);
        }
        transform.translate(-0.5f, -0.5f, -0.5f);

        // Render the turtle
        var colour = turtle.getColour();
        var overlay = turtle.getOverlay();

        if (colour == -1) {
            // Render the turtle using its item model.
            var modelManager = Minecraft.getInstance().getItemRenderer().getItemModelShaper();
            var model = modelManager.getItemModel(turtle.getBlockState().getBlock().asItem());
            if (model == null) model = modelManager.getModelManager().getMissingModel();
            renderModel(transform, buffers, lightmapCoord, overlayLight, model, null);
        } else {
            // Otherwise render it using the colour item.
            renderModel(transform, buffers, lightmapCoord, overlayLight, COLOUR_TURTLE_MODEL, new int[]{ colour });
        }

        // Render the overlay
        var overlayModel = getTurtleOverlayModel(overlay, Holiday.getCurrent() == Holiday.CHRISTMAS);
        if (overlayModel != null) {
            renderModel(transform, buffers, lightmapCoord, overlayLight, overlayModel, null);
        }

        // Render the upgrades
        renderUpgrade(transform, buffers, lightmapCoord, overlayLight, turtle, TurtleSide.LEFT, partialTicks);
        renderUpgrade(transform, buffers, lightmapCoord, overlayLight, turtle, TurtleSide.RIGHT, partialTicks);

        transform.popPose();
    }

    private void renderUpgrade(PoseStack transform, MultiBufferSource buffers, int lightmapCoord, int overlayLight, TurtleBlockEntity turtle, TurtleSide side, float f) {
        var upgrade = turtle.getUpgrade(side);
        if (upgrade == null) return;
        transform.pushPose();

        var toolAngle = turtle.getToolRenderAngle(side, f);
        transform.translate(0.0f, 0.5f, 0.5f);
        transform.mulPose(Axis.XN.rotationDegrees(toolAngle));
        transform.translate(0.0f, -0.5f, -0.5f);

        var model = TurtleUpgradeModellers.getModel(upgrade, turtle.getAccess(), side);
        applyTransformation(transform, model.getMatrix());
        renderModel(transform, buffers, lightmapCoord, overlayLight, model.getModel(), null);

        transform.popPose();
    }

    private void renderModel(PoseStack transform, MultiBufferSource buffers, int lightmapCoord, int overlayLight, ResourceLocation modelLocation, @Nullable int[] tints) {
        var modelManager = Minecraft.getInstance().getItemRenderer().getItemModelShaper().getModelManager();
        renderModel(transform, buffers, lightmapCoord, overlayLight, ClientPlatformHelper.get().getModel(modelManager, modelLocation), tints);
    }

    /**
     * Render a block model.
     *
     * @param transform     The current matrix stack.
     * @param renderer      The buffer to write to.
     * @param lightmapCoord The current lightmap coordinate.
     * @param overlayLight  The overlay light.
     * @param model         The model to render.
     * @param tints         Tints for the quads, as an array of RGB values.
     * @see net.minecraft.client.renderer.block.ModelBlockRenderer#renderModel
     */
    private void renderModel(PoseStack transform, MultiBufferSource renderer, int lightmapCoord, int overlayLight, BakedModel model, @Nullable int[] tints) {
        ClientPlatformHelper.get().renderBakedModel(transform, renderer, model, lightmapCoord, overlayLight, tints);
    }

    private static void applyTransformation(PoseStack stack, Transformation transformation) {
        var trans = transformation.getTranslation();
        stack.translate(trans.x(), trans.y(), trans.z());

        stack.mulPose(transformation.getLeftRotation());

        var scale = transformation.getScale();
        stack.scale(scale.x(), scale.y(), scale.z());

        stack.mulPose(transformation.getRightRotation());
    }
}
