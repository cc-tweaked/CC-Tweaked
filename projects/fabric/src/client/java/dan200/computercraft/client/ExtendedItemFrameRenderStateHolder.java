// SPDX-FileCopyrightText: 2025 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dan200.computercraft.client.render.ExtendedItemFrameRenderState;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.state.ItemFrameRenderState;

/**
 * An interface implemented on {@link ItemFrameRenderState} to provide a {@link ExtendedItemFrameRenderState}.
 *
 * @see ClientHooks#onRenderItemFrame(PoseStack, MultiBufferSource, ItemFrameRenderState, ExtendedItemFrameRenderState, int)
 */
public interface ExtendedItemFrameRenderStateHolder {
    /**
     * Get or create the CC-specific render state.
     *
     * @return The CC-specific render state.
     */
    ExtendedItemFrameRenderState computercraft$state();
}
