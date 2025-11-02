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
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.state.LevelRenderState;
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
     * @see LevelRenderer#renderBlockOutline(MultiBufferSource.BufferSource, PoseStack, boolean, LevelRenderState)
     */
    public static void render(PoseStack transform, MultiBufferSource bufferSource, Renderer renderer) {
        var highContrast = Minecraft.getInstance().options.highContrastBlockOutline().get();
        if (highContrast) {
            renderer.render(transform, bufferSource.getBuffer(RenderType.secondaryBlockOutline()), 0xff000000);
        }

        var colour = highContrast ? CommonColors.HIGH_CONTRAST_DIAMOND : ARGB.color(0x66, CommonColors.BLACK);
        renderer.render(transform, bufferSource.getBuffer(RenderType.lines()), colour);
    }

    @FunctionalInterface
    public interface Renderer {
        void render(PoseStack transform, VertexConsumer buffer, int colour);
    }
}
