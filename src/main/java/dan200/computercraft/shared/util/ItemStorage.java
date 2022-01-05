/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.util;

import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nonnull;

/**
 * The most cutesy alternative of {@code IItemHandler} the world has ever seen.
 */
public interface ItemStorage
{
    static ItemStorage wrap( Container inventory )
    {
        return new InventoryWrapper( inventory );
    }

    static ItemStorage wrap( @Nonnull WorldlyContainer inventory, @Nonnull Direction facing )
    {
        return new SidedInventoryWrapper( inventory, facing );
    }

    static ItemStorage wrap( @Nonnull Container inventory, @Nonnull Direction facing )
    {
        return inventory instanceof WorldlyContainer ? new SidedInventoryWrapper( (WorldlyContainer) inventory, facing ) : new InventoryWrapper( inventory );
    }

    static boolean areStackable( @Nonnull ItemStack a, @Nonnull ItemStack b )
    {
        return a == b || (a.getItem() == b.getItem() && ItemStack.tagMatches( a, b ));
    }

    int size();

    @Nonnull
    ItemStack getStack( int slot );

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
        private final Container inventory;

        InventoryWrapper( Container inventory )
        {
            this.inventory = inventory;
        }

        @Override
        public int size()
        {
            return inventory.getContainerSize();
        }

        @Override
        @Nonnull
        public ItemStack getStack( int slot )
        {
            return inventory.getItem( slot );
        }

        @Override
        @Nonnull
        public ItemStack take( int slot, int limit, @Nonnull ItemStack filter, boolean simulate )
        {
            ItemStack existing = inventory.getItem( slot );
            if( existing.isEmpty() || !canExtract( slot, existing ) || (!filter.isEmpty() && !areStackable( existing, filter )) )
            {
                return ItemStack.EMPTY;
            }

            if( simulate )
            {
                existing = existing.copy();
                if( existing.getCount() > limit )
                {
                    existing.setCount( limit );
                }
                return existing;
            }
            else if( existing.getCount() < limit )
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

        protected boolean canExtract( int slot, ItemStack stack )
        {
            return true;
        }

        private void setAndDirty( int slot, @Nonnull ItemStack stack )
        {
            inventory.setItem( slot, stack );
            inventory.setChanged();
        }

        @Override
        @Nonnull
        public ItemStack store( int slot, @Nonnull ItemStack stack, boolean simulate )
        {
            if( stack.isEmpty() || !inventory.canPlaceItem( slot, stack ) )
            {
                return stack;
            }

            ItemStack existing = inventory.getItem( slot );
            if( existing.isEmpty() )
            {
                int limit = Math.min( stack.getMaxStackSize(), inventory.getMaxStackSize() );
                if( limit <= 0 )
                {
                    return stack;
                }

                if( stack.getCount() < limit )
                {
                    if( !simulate )
                    {
                        setAndDirty( slot, stack );
                    }
                    return ItemStack.EMPTY;
                }
                else
                {
                    stack = stack.copy();
                    ItemStack insert = stack.split( limit );
                    if( !simulate )
                    {
                        setAndDirty( slot, insert );
                    }
                    return stack;
                }
            }
            else if( areStackable( stack, existing ) )
            {
                int limit = Math.min( existing.getMaxStackSize(), inventory.getMaxStackSize() ) - existing.getCount();
                if( limit <= 0 )
                {
                    return stack;
                }

                if( stack.getCount() < limit )
                {
                    if( !simulate )
                    {
                        existing.grow( stack.getCount() );
                        setAndDirty( slot, existing );
                    }
                    return ItemStack.EMPTY;
                }
                else
                {
                    stack = stack.copy();
                    stack.shrink( limit );
                    if( !simulate )
                    {
                        existing.grow( limit );
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
        private final WorldlyContainer inventory;
        private final Direction facing;

        SidedInventoryWrapper( WorldlyContainer inventory, Direction facing )
        {
            super( inventory );
            this.inventory = inventory;
            this.facing = facing;
        }

        @Override
        protected boolean canExtract( int slot, ItemStack stack )
        {
            return super.canExtract( slot, stack ) && inventory.canTakeItemThroughFace( slot, stack, facing );
        }

        @Override
        public int size()
        {
            return inventory.getSlotsForFace( facing ).length;
        }

        @Nonnull
        @Override
        public ItemStack take( int slot, int limit, @Nonnull ItemStack filter, boolean simulate )
        {
            int[] slots = inventory.getSlotsForFace( facing );
            return slot >= 0 && slot < slots.length ? super.take( slots[slot], limit, filter, simulate ) : ItemStack.EMPTY;
        }

        @Nonnull
        @Override
        public ItemStack store( int slot, @Nonnull ItemStack stack, boolean simulate )
        {
            int[] slots = inventory.getSlotsForFace( facing );
            if( slot < 0 || slot >= slots.length )
            {
                return stack;
            }

            int mappedSlot = slots[slot];
            if( !inventory.canPlaceItemThroughFace( slot, stack, facing ) )
            {
                return stack;
            }
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

        @Override
        @Nonnull
        public ItemStack getStack( int slot )
        {
            if( slot < start || slot >= start + size )
            {
                return ItemStack.EMPTY;
            }
            return parent.getStack( slot - start );
        }

        @Nonnull
        @Override
        public ItemStack take( int slot, int limit, @Nonnull ItemStack filter, boolean simulate )
        {
            if( slot < start || slot >= start + size )
            {
                return ItemStack.EMPTY;
            }
            return parent.take( slot - start, limit, filter, simulate );
        }

        @Nonnull
        @Override
        public ItemStack store( int slot, @Nonnull ItemStack stack, boolean simulate )
        {
            if( slot < start || slot >= start + size )
            {
                return stack;
            }
            return parent.store( slot - start, stack, simulate );
        }

        @Override
        public ItemStorage view( int start, int size )
        {
            return new View( parent, this.start + start, size );
        }
    }
}
