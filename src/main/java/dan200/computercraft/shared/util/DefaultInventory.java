/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.util;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;


public interface DefaultInventory extends Container {
    @Override
    default int getMaxStackSize() {
        return 64;
    }

    @Override
    default void startOpen(Player player) {
    }

    @Override
    default void stopOpen(Player player) {
    }

    @Override
    default boolean canPlaceItem(int slot, ItemStack stack) {
        return true;
    }
}
