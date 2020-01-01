/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.util;

import net.minecraft.inventory.ISidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;

import javax.annotation.Nonnull;

public interface DefaultSidedInventory extends DefaultInventory, ISidedInventory
{
    @Override
    default boolean canInsertItem( int slot, @Nonnull ItemStack stack, @Nonnull EnumFacing side )
    {
        return isItemValidForSlot( slot, stack );
    }

    @Override
    default boolean canExtractItem( int slot, @Nonnull ItemStack stack, @Nonnull EnumFacing side )
    {
        return true;
    }
}
