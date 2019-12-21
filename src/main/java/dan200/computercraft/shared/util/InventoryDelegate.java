/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.util;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.ITextComponent;

import javax.annotation.Nonnull;

/**
 * Provides a delegate over inventories.
 *
 * This may be used both on {@link net.minecraft.tileentity.TileEntity}s to redirect the inventory to another tile,
 * and by other interfaces to have inventories which change their backing store.
 */
@FunctionalInterface
public interface InventoryDelegate extends IInventory
{
    IInventory getInventory();

    @Override
    default int getSizeInventory()
    {
        return getInventory().getSizeInventory();
    }

    @Override
    default boolean isEmpty()
    {
        return getInventory().isEmpty();
    }

    @Nonnull
    @Override
    default ItemStack getStackInSlot( int slot )
    {
        return getInventory().getStackInSlot( slot );
    }

    @Nonnull
    @Override
    default ItemStack decrStackSize( int slot, int count )
    {
        return getInventory().decrStackSize( slot, count );
    }

    @Nonnull
    @Override
    default ItemStack removeStackFromSlot( int slot )
    {
        return getInventory().removeStackFromSlot( slot );
    }

    @Override
    default void setInventorySlotContents( int slot, ItemStack stack )
    {
        getInventory().setInventorySlotContents( slot, stack );
    }

    @Override
    default int getInventoryStackLimit()
    {
        return getInventory().getInventoryStackLimit();
    }

    @Override
    default void markDirty()
    {
        getInventory().markDirty();
    }

    @Override
    default boolean isUsableByPlayer( @Nonnull EntityPlayer player )
    {
        return getInventory().isUsableByPlayer( player );
    }

    @Override
    default void openInventory( @Nonnull EntityPlayer player )
    {
        getInventory().openInventory( player );
    }

    @Override
    default void closeInventory( @Nonnull EntityPlayer player )
    {
        getInventory().closeInventory( player );
    }

    @Override
    default boolean isItemValidForSlot( int slot, @Nonnull ItemStack stack )
    {
        return getInventory().isItemValidForSlot( slot, stack );
    }

    @Override
    default int getField( int id )
    {
        return getInventory().getField( id );
    }

    @Override
    default void setField( int id, int val )
    {
        getInventory().setField( id, val );

    }

    @Override
    default int getFieldCount()
    {
        return getInventory().getFieldCount();
    }

    @Override
    default void clear()
    {
        getInventory().clear();
    }

    @Nonnull
    @Override
    default String getName()
    {
        return getInventory().getName();
    }

    @Override
    default boolean hasCustomName()
    {
        return getInventory().hasCustomName();
    }

    @Nonnull
    @Override
    default ITextComponent getDisplayName()
    {
        return getInventory().getDisplayName();
    }
}
