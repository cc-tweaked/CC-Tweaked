/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.util;

import dan200.computercraft.ComputerCraft;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Direction;

import javax.annotation.Nonnull;

/**
 * The most cutesy alternative of {@code IItemHandler} the world has ever seen.
 */
public interface ItemStorage
{
    int size();

    @Nonnull
    ItemStack take( int slot, int limit, @Nonnull ItemStack filter, boolean simulate );

    @Nonnull
    ItemStack store( int slot, @Nonnull ItemStack stack, boolean simulate );

    default ItemStorage view( int start, int size )
    {
        return new View( this, start, size );
    }

    class InventoryWrapper implements ItemStorage
    {
        private final Inventory inventory;

        InventoryWrapper( Inventory inventory )
        {
            this.inventory = inventory;
        }

        private void setAndDirty( int slot, @Nonnull ItemStack stack )
        {
            inventory.setInvStack( slot, stack );
            inventory.markDirty();
        }

        protected boolean canExtract( int slot, ItemStack stack )
        {
            return true;
        }

        @Override
        public int size()
        {
            return inventory.getInvSize();
        }

        @Override
        @Nonnull
        public ItemStack take( int slot, int limit, @Nonnull ItemStack filter, boolean simulate )
        {
            ItemStack existing = inventory.getInvStack( slot );
            if( existing.isEmpty() || !canExtract( slot, existing )
                || (!filter.isEmpty() && !areStackable( existing, filter )) )
            {
                return ItemStack.EMPTY;
            }

            if( simulate )
            {
                existing = existing.copy();
                if( existing.getAmount() > limit ) existing.setAmount( limit );
                return existing;
            }
            else if( existing.getAmount() < limit )
            {
                setAndDirty( slot, ItemStack.EMPTY );
                return existing;
            }
            else
            {
                ItemStack result = existing.split( limit );
                setAndDirty( slot, existing );
                return result;
            }
        }

        @Override
        @Nonnull
        public ItemStack store( int slot, @Nonnull ItemStack stack, boolean simulate )
        {
            if( stack.isEmpty() || !inventory.isValidInvStack( slot, stack ) ) return stack;

            ItemStack existing = inventory.getInvStack( slot );
            if( existing.isEmpty() )
            {
                int limit = Math.min( stack.getMaxAmount(), inventory.getInvMaxStackAmount() );
                if( limit <= 0 ) return stack;

                if( stack.getAmount() < limit )
                {
                    if( !simulate ) setAndDirty( slot, stack );
                    return ItemStack.EMPTY;
                }
                else
                {
                    stack = stack.copy();
                    ItemStack insert = stack.split( limit );
                    if( !simulate ) setAndDirty( slot, insert );
                    return stack;
                }
            }
            else if( areStackable( stack, existing ) )
            {
                int limit = Math.min( existing.getMaxAmount(), inventory.getInvMaxStackAmount() ) - existing.getAmount();
                if( limit <= 0 ) return stack;

                if( stack.getAmount() < limit )
                {
                    if( !simulate )
                    {
                        existing.addAmount( stack.getAmount() );
                        setAndDirty( slot, existing );
                    }
                    return ItemStack.EMPTY;
                }
                else
                {
                    stack = stack.copy();
                    stack.subtractAmount( limit );
                    if( !simulate )
                    {
                        existing.addAmount( limit );
                        setAndDirty( slot, existing );
                    }
                    return stack;
                }
            }
            else
            {
                return stack;
            }
        }
    }

    class SidedInventoryWrapper extends InventoryWrapper
    {
        private final SidedInventory inventory;
        private final Direction facing;

        SidedInventoryWrapper( SidedInventory inventory, Direction facing )
        {
            super( inventory );
            this.inventory = inventory;
            this.facing = facing;
        }

        @Override
        public int size()
        {
            return inventory.getInvAvailableSlots( facing ).length;
        }

        @Override
        protected boolean canExtract( int slot, ItemStack stack )
        {
            return super.canExtract( slot, stack ) && inventory.canExtractInvStack( slot, stack, facing );
        }

        @Nonnull
        @Override
        public ItemStack take( int slot, int limit, @Nonnull ItemStack filter, boolean simulate )
        {
            int[] slots = inventory.getInvAvailableSlots( facing );
            return slot >= 0 && slot < slots.length ? super.take( slots[slot], limit, filter, simulate ) : ItemStack.EMPTY;
        }

        @Nonnull
        @Override
        public ItemStack store( int slot, @Nonnull ItemStack stack, boolean simulate )
        {
            int[] slots = inventory.getInvAvailableSlots( facing );
            if( slot < 0 || slot >= slots.length ) return stack;

            int mappedSlot = slots[slot];
            if( !inventory.canInsertInvStack( slot, stack, facing ) ) return stack;
            return super.store( mappedSlot, stack, simulate );
        }
    }

    class View implements ItemStorage
    {
        private final ItemStorage parent;
        private final int start;
        private final int size;

        View( ItemStorage parent, int start, int size )
        {
            this.parent = parent;
            this.start = start;
            this.size = size;
        }

        @Override
        public int size()
        {
            return size;
        }

        @Nonnull
        @Override
        public ItemStack take( int slot, int limit, @Nonnull ItemStack filter, boolean simulate )
        {
            if( slot < start || slot >= start + size ) return ItemStack.EMPTY;
            return parent.take( slot - start, limit, filter, simulate );
        }

        @Nonnull
        @Override
        public ItemStack store( int slot, @Nonnull ItemStack stack, boolean simulate )
        {
            if( slot < start || slot >= start + size ) return stack;
            return parent.store( slot - start, stack, simulate );
        }

        @Override
        public ItemStorage view( int start, int size )
        {
            return new View( this.parent, this.start + start, size );
        }
    }

    static ItemStorage wrap( Inventory inventory )
    {
        return new InventoryWrapper( inventory );
    }

    static ItemStorage wrap( @Nonnull SidedInventory inventory, @Nonnull Direction facing )
    {
        return new SidedInventoryWrapper( inventory, facing );
    }

    static ItemStorage wrap( @Nonnull Inventory inventory, @Nonnull Direction facing )
    {
        return inventory instanceof SidedInventory
            ? new SidedInventoryWrapper( (SidedInventory) inventory, facing )
            : new InventoryWrapper( inventory );
    }

    static boolean areStackable( @Nonnull ItemStack a, @Nonnull ItemStack b )
    {
        return a == b || (a.getItem() == b.getItem() && ItemStack.areTagsEqual( a, b ));
    }
}
