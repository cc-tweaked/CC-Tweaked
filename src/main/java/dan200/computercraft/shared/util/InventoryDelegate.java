/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.util;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import javax.annotation.Nonnull;
import java.util.Set;

/**
 * Provides a delegate over inventories.
 *
 * This may be used both on {@link net.minecraft.tileentity.TileEntity}s to redirect the inventory to another tile, and by other interfaces to have
 * inventories which change their backing store.
 */
@FunctionalInterface
public interface InventoryDelegate extends Inventory
{
    @Override
    default int size()
    {
        return this.getInventory().size();
    }

    Inventory getInventory();

    @Override
    default boolean isEmpty()
    {
        return this.getInventory().isEmpty();
    }

    @Nonnull
    @Override
    default ItemStack getStack( int slot )
    {
        return this.getInventory().getStack( slot );
    }

    @Nonnull
    @Override
    default ItemStack removeStack( int slot, int count )
    {
        return this.getInventory().removeStack( slot, count );
    }

    @Nonnull
    @Override
    default ItemStack removeStack( int slot )
    {
        return this.getInventory().removeStack( slot );
    }

    @Override
    default void setStack( int slot, @Nonnull ItemStack stack )
    {
        this.getInventory().setStack( slot, stack );
    }

    @Override
    default int getMaxCountPerStack()
    {
        return this.getInventory().getMaxCountPerStack();
    }

    @Override
    default void markDirty()
    {
        this.getInventory().markDirty();
    }

    @Override
    default boolean canPlayerUse( @Nonnull PlayerEntity player )
    {
        return this.getInventory().canPlayerUse( player );
    }

    @Override
    default void onOpen( @Nonnull PlayerEntity player )
    {
        this.getInventory().onOpen( player );
    }

    @Override
    default void onClose( @Nonnull PlayerEntity player )
    {
        this.getInventory().onClose( player );
    }

    @Override
    default boolean isValid( int slot, @Nonnull ItemStack stack )
    {
        return this.getInventory().isValid( slot, stack );
    }

    @Override
    default int count( @Nonnull Item stack )
    {
        return this.getInventory().count( stack );
    }

    @Override
    default boolean containsAny( @Nonnull Set<Item> set )
    {
        return this.getInventory().containsAny( set );
    }

    @Override
    default void clear()
    {
        this.getInventory().clear();
    }
}
