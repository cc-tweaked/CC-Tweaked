// Copyright Daniel Ratcliffe, 2011-2022. This API may be redistributed unmodified and in full only.
//
// SPDX-License-Identifier: LicenseRef-CCPL

package dan200.computercraft.api.media;

import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

/**
 * This interface is used to provide {@link IMedia} implementations for {@link ItemStack}.
 *
 * @see dan200.computercraft.api.ComputerCraftAPI#registerMediaProvider(MediaProvider)
 */
@FunctionalInterface
public interface MediaProvider {
    /**
     * Produce an IMedia implementation from an ItemStack.
     *
     * @param stack The stack from which to extract the media information.
     * @return An {@link IMedia} implementation, or {@code null} if the item is not something you wish to handle
     * @see dan200.computercraft.api.ComputerCraftAPI#registerMediaProvider(MediaProvider)
     */
    @Nullable
    IMedia getMedia(ItemStack stack);
}
