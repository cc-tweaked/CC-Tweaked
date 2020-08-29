/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.util;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.inventory.SidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Direction;

public interface DefaultSidedInventory extends DefaultInventory, SidedInventory {
    @Override
    default boolean canInsert(int slot, @Nonnull ItemStack stack, @Nullable Direction side) {
        return this.isValid(slot, stack);
    }

    @Override
    default boolean canExtract(int slot, @Nonnull ItemStack stack, @Nonnull Direction side) {
        return true;
    }
}
