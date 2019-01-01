/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.util;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;

import javax.annotation.Nonnull;

public interface DefaultInventory extends IInventory
{
    @Override
    default int getInventoryStackLimit()
    {
        return 64;
    }

    @Override
    default void openInventory( @Nonnull EntityPlayer player )
    {
    }

    @Override
    default void closeInventory( @Nonnull EntityPlayer player )
    {
    }

    @Override
    default boolean isItemValidForSlot( int slot, @Nonnull ItemStack stack )
    {
        return true;
    }

    @Override
    default int getField( int field )
    {
        return 0;
    }

    @Override
    default void setField( int field, int value )
    {
    }

    @Override
    default int getFieldCount()
    {
        return 0;
    }
}
