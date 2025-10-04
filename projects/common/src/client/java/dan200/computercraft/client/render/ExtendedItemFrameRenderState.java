// SPDX-FileCopyrightText: 2025 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import dan200.computercraft.shared.ModRegistry;
import dan200.computercraft.shared.media.items.PrintoutData;
import dan200.computercraft.shared.media.items.PrintoutItem;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.state.ItemFrameRenderState;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * Additional render state attached to a {@link ItemFrameRenderState}.
 *
 * @see dan200.computercraft.client.ClientHooks#onRenderItemFrame(PoseStack, MultiBufferSource, ItemFrameRenderState, ExtendedItemFrameRenderState, int)
 */
public class ExtendedItemFrameRenderState {
    public @Nullable PrintoutData printoutData;
    public boolean isBook;

    /**
     * Set up the render state from the {@link ItemFrame}'s {@link ItemStack}.
     *
     * @param stack The item frame's item.
     */
    public void setup(ItemStack stack) {
        if (stack.getItem() instanceof PrintoutItem) {
            printoutData = PrintoutData.getOrEmpty(stack);
            isBook = stack.getItem() == ModRegistry.Items.PRINTED_BOOK.get();
        } else {
            printoutData = null;
            isBook = false;
        }
    }
}
