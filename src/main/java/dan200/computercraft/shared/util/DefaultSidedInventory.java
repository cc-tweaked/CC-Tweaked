/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.util;

import net.minecraft.inventory.SidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Direction;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public interface DefaultSidedInventory extends DefaultInventory, SidedInventory
{
    @Override
    default boolean canInsert( int slot, @Nonnull ItemStack stack, @Nullable Direction side )
    {
        return this.isValid( slot, stack );
    }

    @Override
    default boolean canExtract( int slot, @Nonnull ItemStack stack, @Nonnull Direction side )
    {
        return true;
    }
}
