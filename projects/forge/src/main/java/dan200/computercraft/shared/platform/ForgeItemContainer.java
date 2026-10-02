// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.platform;

import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;

import java.util.Iterator;
import java.util.Objects;

public final class ForgeItemContainer implements ItemContainer {
    private final IItemHandler handler;

    public ForgeItemContainer(IItemHandler handler) {
        this.handler = handler;
    }

    @Override
    public int size() {
        return handler.getSlots();
    }

    @Override
    public Iterator<ItemStack> contents() {
        class ContentsIterator implements Iterator<ItemStack> {
            private int slot;
            private final int size;
            private final IItemHandler handler;

            ContentsIterator(IItemHandler handler) {
                this.handler = handler;
                this.size = handler.getSlots();
            }

            @Override
            public boolean hasNext() {
                return slot < size;
            }

            @Override
            public ItemStack next() {
                return handler.getStackInSlot(this.slot++);
            }
        }

        return new ContentsIterator(handler);
    }

    @Override
    public ItemStack getItem(int slot) {
        return handler.getStackInSlot(slot);
    }

    @Override
    public int getCapacity(int slot) {
        return handler.getSlotLimit(slot);
    }

    @Override
    public ItemContainer singleSlot(int slot) {
        Objects.checkIndex(slot, size());
        return handler instanceof SingleSlotWrapper ? this : new ForgeItemContainer(new SingleSlotWrapper(handler, slot));
    }

    @Override
    public int moveTo(ItemContainer destination, int maxAmount) {
        var hasItems = false;
        var destHandler = ((ForgeItemContainer) destination).handler;
        for (int fromSlot = 0, fromSize = handler.getSlots(); fromSlot < fromSize; fromSlot++) {
            var result = move(handler, fromSlot, destHandler, maxAmount);
            if (result >= 0) return result;

            if (result == NO_SPACE) hasItems = true;
        }

        return hasItems ? NO_SPACE : NO_ITEMS;
    }

    @Override
    public int moveTo(int fromSlot, ItemContainer destination, int maxAmount) {
        return move(handler, fromSlot, ((ForgeItemContainer) destination).handler, maxAmount);
    }

    public static int move(IItemHandler from, int fromSlot, IItemHandler to, int maxAmount) {
        var stack = from.extractItem(fromSlot, saturatingToInt(maxAmount), true);
        if (stack.isEmpty()) return NO_ITEMS;

        // Pick the first item in the inventory to be the one we transfer, skipping those that match.
        if (stack.getMaxStackSize() < maxAmount) maxAmount = stack.getMaxStackSize();

        long moved = 0;
        for (int toSlot = 0, toSize = to.getSlots(); toSlot < toSize; toSlot++) {
            var oldCount = stack.getCount();
            stack = to.insertItem(toSlot, stack, false);

            var transferred = oldCount - stack.getCount();
            if (transferred == 0) continue;

            var extracted = from.extractItem(fromSlot, transferred, false);

            moved += transferred;

            // We failed to extract as much as we should have. This should never happen, but goodness knows.
            if (extracted.getCount() < transferred) break;

            // Abort if we've extracted all our items, abort.
            if (moved >= maxAmount) break;
            if (stack.isEmpty()) break;
        }

        return moved == 0 ? NO_SPACE : saturatingToInt(moved);
    }

    private static int saturatingToInt(long value) {
        return value > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) value;
    }

    private abstract static class MappedSlotWrapper implements IItemHandler {
        final IItemHandler handler;

        private MappedSlotWrapper(IItemHandler handler) {
            this.handler = handler;
        }

        abstract int mapSlot(int slot);

        @Override
        public final ItemStack getStackInSlot(int slot) {
            return handler.getStackInSlot(mapSlot(slot));
        }

        @Override
        public final int getSlotLimit(int slot) {
            return handler.getSlotLimit(mapSlot(slot));
        }

        @Override
        public final ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return handler.insertItem(mapSlot(slot), stack, simulate);
        }

        @Override
        public final ItemStack extractItem(int slot, int limit, boolean simulate) {
            return handler.extractItem(mapSlot(slot), limit, simulate);
        }

        @Override
        public final boolean isItemValid(int slot, ItemStack stack) {
            return handler.isItemValid(mapSlot(slot), stack);
        }
    }

    private static final class SingleSlotWrapper extends MappedSlotWrapper {
        private final int actualSlot;

        private SingleSlotWrapper(IItemHandler handler, int slot) {
            super(handler);
            this.actualSlot = slot;
        }

        @Override
        public int getSlots() {
            return 1;
        }

        @Override
        int mapSlot(int slot) {
            Objects.checkIndex(slot, 1);
            return actualSlot;
        }
    }

    private static final class RotatedWrapper extends MappedSlotWrapper {
        private final int offset;

        RotatedWrapper(IItemHandler handler, int offset) {
            super(handler);
            this.offset = offset;
        }

        @Override
        public int getSlots() {
            return handler.getSlots();
        }

        @Override
        protected int mapSlot(int slot) {
            var size = getSlots();
            Objects.checkIndex(slot, size);

            var actualSlot = slot + offset;
            return actualSlot >= size ? actualSlot - size : actualSlot;
        }
    }

    static ItemContainer rotated(IItemHandler handler, int offset) {
        return new ForgeItemContainer(new RotatedWrapper(handler, offset));
    }
}
