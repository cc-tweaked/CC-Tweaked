/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.util;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import javax.annotation.Nonnull;
import java.util.Set;

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
    default boolean isUsableByPlayer( @Nonnull PlayerEntity player )
    {
        return getInventory().isUsableByPlayer( player );
    }

    @Override
    default void openInventory( @Nonnull PlayerEntity player )
    {
        getInventory().openInventory( player );
    }

    @Override
    default void closeInventory( @Nonnull PlayerEntity player )
    {
        getInventory().closeInventory( player );
    }

    @Override
    default boolean isItemValidForSlot( int slot, @Nonnull ItemStack stack )
    {
        return getInventory().isItemValidForSlot( slot, stack );
    }

    @Override
    default void clear()
    {
        getInventory().clear();
    }

    @Override
    default int count( @Nonnull Item stack )
    {
        return getInventory().count( stack );
    }

    @Override
    default boolean hasAny( @Nonnull Set<Item> set )
    {
        return getInventory().hasAny( set );
    }
}
