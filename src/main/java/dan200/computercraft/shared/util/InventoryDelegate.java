/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
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
    default int getContainerSize()
    {
        return getInventory().getContainerSize();
    }

    @Override
    default boolean isEmpty()
    {
        return getInventory().isEmpty();
    }

    @Nonnull
    @Override
    default ItemStack getItem( int slot )
    {
        return getInventory().getItem( slot );
    }

    @Nonnull
    @Override
    default ItemStack removeItem( int slot, int count )
    {
        return getInventory().removeItem( slot, count );
    }

    @Nonnull
    @Override
    default ItemStack removeItemNoUpdate( int slot )
    {
        return getInventory().removeItemNoUpdate( slot );
    }

    @Override
    default void setItem( int slot, @Nonnull ItemStack stack )
    {
        getInventory().setItem( slot, stack );
    }

    @Override
    default int getMaxStackSize()
    {
        return getInventory().getMaxStackSize();
    }

    @Override
    default void setChanged()
    {
        getInventory().setChanged();
    }

    @Override
    default boolean stillValid( @Nonnull PlayerEntity player )
    {
        return getInventory().stillValid( player );
    }

    @Override
    default void startOpen( @Nonnull PlayerEntity player )
    {
        getInventory().startOpen( player );
    }

    @Override
    default void stopOpen( @Nonnull PlayerEntity player )
    {
        getInventory().stopOpen( player );
    }

    @Override
    default boolean canPlaceItem( int slot, @Nonnull ItemStack stack )
    {
        return getInventory().canPlaceItem( slot, stack );
    }

    @Override
    default void clearContent()
    {
        getInventory().clearContent();
    }

    @Override
    default int countItem( @Nonnull Item stack )
    {
        return getInventory().countItem( stack );
    }

    @Override
    default boolean hasAnyOf( @Nonnull Set<Item> set )
    {
        return getInventory().hasAnyOf( set );
    }
}
