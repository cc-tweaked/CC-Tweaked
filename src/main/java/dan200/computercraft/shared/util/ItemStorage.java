/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.util;

import javax.annotation.Nonnull;

import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Direction;

/**
 * The most cutesy alternative of {@code IItemHandler} the world has ever seen.
 */
public interface ItemStorage {
    static ItemStorage wrap(Inventory inventory) {
        return new InventoryWrapper(inventory);
    }

    static ItemStorage wrap(@Nonnull SidedInventory inventory, @Nonnull Direction facing) {
        return new SidedInventoryWrapper(inventory, facing);
    }

    static ItemStorage wrap(@Nonnull Inventory inventory, @Nonnull Direction facing) {
        return inventory instanceof SidedInventory ? new SidedInventoryWrapper((SidedInventory) inventory, facing) : new InventoryWrapper(inventory);
    }

    static boolean areStackable(@Nonnull ItemStack a, @Nonnull ItemStack b) {
        return a == b || (a.getItem() == b.getItem() && ItemStack.areTagsEqual(a, b));
    }

    int size();

    @Nonnull
    ItemStack getStack(int slot);

    @Nonnull
    ItemStack take(int slot, int limit, @Nonnull ItemStack filter, boolean simulate);

    @Nonnull
    ItemStack store(int slot, @Nonnull ItemStack stack, boolean simulate);

    default ItemStorage view(int start, int size) {
        return new View(this, start, size);
    }

    class InventoryWrapper implements ItemStorage {
        private final Inventory inventory;

        InventoryWrapper(Inventory inventory) {
            this.inventory = inventory;
        }

        @Override
        public int size() {
            return this.inventory.size();
        }

        @Override
        @Nonnull
        public ItemStack getStack(int slot) {
            return this.inventory.getStack(slot);
        }

        @Override
        @Nonnull
        public ItemStack take(int slot, int limit, @Nonnull ItemStack filter, boolean simulate) {
            ItemStack existing = this.inventory.getStack(slot);
            if (existing.isEmpty() || !this.canExtract(slot, existing) || (!filter.isEmpty() && !areStackable(existing, filter))) {
                return ItemStack.EMPTY;
            }

            if (simulate) {
                existing = existing.copy();
                if (existing.getCount() > limit) {
                    existing.setCount(limit);
                }
                return existing;
            } else if (existing.getCount() < limit) {
                this.setAndDirty(slot, ItemStack.EMPTY);
                return existing;
            } else {
                ItemStack result = existing.split(limit);
                this.setAndDirty(slot, existing);
                return result;
            }
        }

        protected boolean canExtract(int slot, ItemStack stack) {
            return true;
        }

        private void setAndDirty(int slot, @Nonnull ItemStack stack) {
            this.inventory.setStack(slot, stack);
            this.inventory.markDirty();
        }

        @Override
        @Nonnull
        public ItemStack store(int slot, @Nonnull ItemStack stack, boolean simulate) {
            if (stack.isEmpty() || !this.inventory.isValid(slot, stack)) {
                return stack;
            }

            ItemStack existing = this.inventory.getStack(slot);
            if (existing.isEmpty()) {
                int limit = Math.min(stack.getMaxCount(), this.inventory.getMaxCountPerStack());
                if (limit <= 0) {
                    return stack;
                }

                if (stack.getCount() < limit) {
                    if (!simulate) {
                        this.setAndDirty(slot, stack);
                    }
                    return ItemStack.EMPTY;
                } else {
                    stack = stack.copy();
                    ItemStack insert = stack.split(limit);
                    if (!simulate) {
                        this.setAndDirty(slot, insert);
                    }
                    return stack;
                }
            } else if (areStackable(stack, existing)) {
                int limit = Math.min(existing.getMaxCount(), this.inventory.getMaxCountPerStack()) - existing.getCount();
                if (limit <= 0) {
                    return stack;
                }

                if (stack.getCount() < limit) {
                    if (!simulate) {
                        existing.increment(stack.getCount());
                        this.setAndDirty(slot, existing);
                    }
                    return ItemStack.EMPTY;
                } else {
                    stack = stack.copy();
                    stack.decrement(limit);
                    if (!simulate) {
                        existing.increment(limit);
                        this.setAndDirty(slot, existing);
                    }
                    return stack;
                }
            } else {
                return stack;
            }
        }
    }

    class SidedInventoryWrapper extends InventoryWrapper {
        private final SidedInventory inventory;
        private final Direction facing;

        SidedInventoryWrapper(SidedInventory inventory, Direction facing) {
            super(inventory);
            this.inventory = inventory;
            this.facing = facing;
        }

        @Override
        protected boolean canExtract(int slot, ItemStack stack) {
            return super.canExtract(slot, stack) && this.inventory.canExtract(slot, stack, this.facing);
        }

        @Override
        public int size() {
            return this.inventory.getAvailableSlots(this.facing).length;
        }

        @Nonnull
        @Override
        public ItemStack take(int slot, int limit, @Nonnull ItemStack filter, boolean simulate) {
            int[] slots = this.inventory.getAvailableSlots(this.facing);
            return slot >= 0 && slot < slots.length ? super.take(slots[slot], limit, filter, simulate) : ItemStack.EMPTY;
        }

        @Nonnull
        @Override
        public ItemStack store(int slot, @Nonnull ItemStack stack, boolean simulate) {
            int[] slots = this.inventory.getAvailableSlots(this.facing);
            if (slot < 0 || slot >= slots.length) {
                return stack;
            }

            int mappedSlot = slots[slot];
            if (!this.inventory.canInsert(slot, stack, this.facing)) {
                return stack;
            }
            return super.store(mappedSlot, stack, simulate);
        }
    }

    class View implements ItemStorage {
        private final ItemStorage parent;
        private final int start;
        private final int size;

        View(ItemStorage parent, int start, int size) {
            this.parent = parent;
            this.start = start;
            this.size = size;
        }

        @Override
        public int size() {
            return this.size;
        }

        @Override
        @Nonnull
        public ItemStack getStack(int slot) {
            if (slot < this.start || slot >= this.start + this.size) {
                return ItemStack.EMPTY;
            }
            return this.parent.getStack(slot - this.start );
        }

        @Nonnull
        @Override
        public ItemStack take(int slot, int limit, @Nonnull ItemStack filter, boolean simulate) {
            if (slot < this.start || slot >= this.start + this.size) {
                return ItemStack.EMPTY;
            }
            return this.parent.take(slot - this.start, limit, filter, simulate);
        }

        @Nonnull
        @Override
        public ItemStack store(int slot, @Nonnull ItemStack stack, boolean simulate) {
            if (slot < this.start || slot >= this.start + this.size) {
                return stack;
            }
            return this.parent.store(slot - this.start, stack, simulate);
        }

        @Override
        public ItemStorage view(int start, int size) {
            return new View(this.parent, this.start + start, size);
        }
    }
}
