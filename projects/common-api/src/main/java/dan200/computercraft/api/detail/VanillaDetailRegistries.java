// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.api.detail;

import dan200.computercraft.impl.ComputerCraftAPIService;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

/**
 * {@link DetailRegistry}s for built-in Minecraft types.
 */
public class VanillaDetailRegistries {
    /**
     * Provides details for {@link ItemStack}s.
     * <p>
     * This instance's {@link DetailRegistry#getBasicDetails(Object)} is thread safe (assuming the stack is immutable)
     * and may be called from the computer thread.
     */
    public static final DetailRegistry<ItemStack> ITEM_STACK = ComputerCraftAPIService.get().getItemStackDetailRegistry();

    /**
     * Provides details for {@link BlockReference}, a reference to a {@link Block} in the world.
     * <p>
     * This instance's {@link DetailRegistry#getBasicDetails(Object)} is thread safe and may be called from the computer
     * thread.
     */
    public static final DetailRegistry<BlockReference> BLOCK_IN_WORLD = ComputerCraftAPIService.get().getBlockInWorldDetailRegistry();
}
