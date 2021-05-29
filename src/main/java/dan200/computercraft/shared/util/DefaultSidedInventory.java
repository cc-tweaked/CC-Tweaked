/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.util;

import net.minecraft.inventory.ISidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Direction;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public interface DefaultSidedInventory extends DefaultInventory, ISidedInventory
{
    @Override
    default boolean canPlaceItemThroughFace( int slot, @Nonnull ItemStack stack, @Nullable Direction side )
    {
        return canPlaceItem( slot, stack );
    }

    @Override
    default boolean canTakeItemThroughFace( int slot, @Nonnull ItemStack stack, @Nonnull Direction side )
    {
        return true;
    }
}
