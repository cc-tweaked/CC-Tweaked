// SPDX-FileCopyrightText: 2025 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dan200.computercraft.client.ClientHooks;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.util.ARGB;
import net.minecraft.util.CommonColors;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Utilities for rendering block outline.
 *
 * @see ClientHooks#drawHighlight(Camera, BlockHitResult)
 */
public final class BlockOutlineRenderer {
    private BlockOutlineRenderer() {
    }

    /**
     * Render a block outline, handling both normal and high-contrast modes.
     *
     * @param transform    The current transformations.
     * @param bufferSource The buffer source.
     * @param renderer     The function to render a highlight.
     * @param camera       The current camera.
     * @param hit          The hit result of the block we're rendering.
     * @see LevelRenderer#submitBlockOutline(PoseStack, SubmitNodeCollector, LevelRenderState)
     */
    public static void render(PoseStack transform, SubmitNodeCollector bufferSource, Renderer renderer, Camera camera, BlockHitResult hit) {
        var cameraPos = camera.position();
        var xOffset = hit.getBlockPos().getX() - cameraPos.x();
        var yOffset = hit.getBlockPos().getY() - cameraPos.y();
        var zOffset = hit.getBlockPos().getZ() - cameraPos.z();

        transform.pushPose();
        transform.translate(xOffset, yOffset, zOffset);

        var highContrast = Minecraft.getInstance().options.highContrastBlockOutline().get();
        if (highContrast) {
            bufferSource.submitCustomGeometry(transform, RenderTypes.secondaryBlockOutline(), (p, b) -> renderer.render(p, b, 0xff000000, 7f));
        }

        var colour = highContrast ? CommonColors.HIGH_CONTRAST_DIAMOND : ARGB.color(0x66, CommonColors.BLACK);
        bufferSource.submitCustomGeometry(transform, RenderTypes.lines(), (p, b) -> renderer.render(p, b, colour, Minecraft.getInstance().getWindow().getAppropriateLineWidth()));

        transform.popPose();
    }

    @FunctionalInterface
    public interface Renderer {
        void render(PoseStack.Pose transform, VertexConsumer buffer, int colour, float width);
    }
}
