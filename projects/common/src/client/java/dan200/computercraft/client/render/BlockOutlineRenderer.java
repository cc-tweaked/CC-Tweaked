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
import net.minecraft.util.ARGB;
import net.minecraft.util.CommonColors;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Utilities for rendering block outline.
 *
 * @see ClientHooks#drawHighlight(PoseStack, MultiBufferSource, Camera, BlockHitResult)
 */
public final class BlockOutlineRenderer {
    private BlockOutlineRenderer() {
    }

    /**
     * Render a block outline, handling both normal and high-contrast modes.
     *
     * @param bufferSource The buffer source.
     * @param renderer     The function to render a highlight.
     * @see LevelRenderer#renderBlockOutline(Camera, MultiBufferSource.BufferSource, PoseStack, boolean)
     */
    public static void render(MultiBufferSource bufferSource, Renderer renderer) {
        var highContrast = Minecraft.getInstance().options.highContrastBlockOutline().get();
        if (highContrast) renderer.render(bufferSource.getBuffer(RenderType.secondaryBlockOutline()), 0xff000000);

        var colour = highContrast ? CommonColors.HIGH_CONTRAST_DIAMOND : ARGB.color(0x66, CommonColors.BLACK);
        renderer.render(bufferSource.getBuffer(RenderType.lines()), colour);
    }

    @FunctionalInterface
    public interface Renderer {
        void render(VertexConsumer buffer, int colour);
    }
}
