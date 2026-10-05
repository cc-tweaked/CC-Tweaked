// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.platform;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

import java.util.Iterator;
import java.util.Objects;

public final class ForgeItemContainer implements ItemContainer {
    private final ResourceHandler<ItemResource> handler;

    public ForgeItemContainer(ResourceHandler<ItemResource> handler) {
        this.handler = handler;
    }

    @Override
    public int size() {
        return handler.size();
    }

    @Override
    public Iterator<ItemStack> contents() {
        class ContentsIterator implements Iterator<ItemStack> {
            private int slot;
            private final int size;
            private final ResourceHandler<ItemResource> handler;

            ContentsIterator(ResourceHandler<ItemResource> handler) {
                this.handler = handler;
                this.size = handler.size();
            }

            @Override
            public boolean hasNext() {
                return slot < size;
            }

            @Override
            public ItemStack next() {
                return getStack(handler, slot++);
            }
        }

        return new ContentsIterator(handler);
    }

    private static ItemStack getStack(ResourceHandler<ItemResource> handler, int slot) {
        var resource = handler.getResource(slot);
        var amount = handler.getAmountAsInt(slot);
        return resource.isEmpty() || amount <= 0 ? ItemStack.EMPTY : resource.toStack(amount);
    }

    @Override
    public ItemStack getItem(int slot) {
        return getStack(handler, slot);
    }

    @Override
    public int getCapacity(int slot) {
        return handler.getCapacityAsInt(slot, handler.getResource(slot));
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
        for (int i = 0, size = handler.size(); i < size; i++) {
            var result = move(handler, i, destHandler, maxAmount);
            if (result >= 0) return result;

            if (result == NO_SPACE) hasItems = true;
        }

        return hasItems ? NO_SPACE : NO_ITEMS;
    }

    @Override
    public int moveTo(int fromSlot, ItemContainer destination, int maxAmount) {
        return move(handler, fromSlot, ((ForgeItemContainer) destination).handler, maxAmount);
    }

    private static int move(ResourceHandler<ItemResource> from, int fromSlot, ResourceHandler<ItemResource> to, int maxAmount) {
        var resource = from.getResource(fromSlot);
        if (resource.isEmpty()) return NO_ITEMS;

        // Check how much can be extracted.
        int maxExtracted;
        try (var transaction = Transaction.openRoot()) {
            maxExtracted = from.extract(fromSlot, resource, maxAmount, transaction);
        }
        if (maxExtracted == 0) return NO_ITEMS;

        try (var transaction = Transaction.openRoot()) {
            // Check how much can be inserted.
            var accepted = to.insert(resource, maxExtracted, transaction);
            if (accepted == 0) return NO_SPACE;

            // Extract or rollback.
            if (from.extract(resource, accepted, transaction) == accepted) {
                transaction.commit();
                return accepted;
            }

            return NO_SPACE;
        }
    }

    private abstract static class MappedSlotWrapper implements ResourceHandler<ItemResource> {
        final ResourceHandler<ItemResource> handler;

        private MappedSlotWrapper(ResourceHandler<ItemResource> handler) {
            this.handler = handler;
        }

        abstract int mapSlot(int slot);

        @Override
        public ItemResource getResource(int index) {
            return handler.getResource(mapSlot(index));
        }

        @Override
        public long getAmountAsLong(int index) {
            return handler.getAmountAsLong(mapSlot(index));
        }

        @Override
        public long getCapacityAsLong(int index, ItemResource resource) {
            return handler.getCapacityAsLong(mapSlot(index), resource);
        }

        @Override
        public boolean isValid(int index, ItemResource resource) {
            return handler.isValid(mapSlot(index), resource);
        }

        @Override
        public int insert(int index, ItemResource resource, int amount, TransactionContext transaction) {
            return handler.insert(mapSlot(index), resource, amount, transaction);
        }

        @Override
        public int extract(int index, ItemResource resource, int amount, TransactionContext transaction) {
            return handler.extract(mapSlot(index), resource, amount, transaction);
        }
    }

    private static final class SingleSlotWrapper extends MappedSlotWrapper {
        private final int actualSlot;

        private SingleSlotWrapper(ResourceHandler<ItemResource> handler, int slot) {
            super(handler);
            this.actualSlot = slot;
        }

        @Override
        public int size() {
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

        RotatedWrapper(ResourceHandler<ItemResource> handler, int offset) {
            super(handler);
            this.offset = offset;
        }

        @Override
        public int size() {
            return handler.size();
        }

        @Override
        protected int mapSlot(int slot) {
            var size = size();
            Objects.checkIndex(slot, size);

            var actualSlot = slot + offset;
            return actualSlot >= size ? actualSlot - size : actualSlot;
        }
    }

    static ItemContainer rotated(ResourceHandler<ItemResource> handler, int offset) {
        return new ForgeItemContainer(new RotatedWrapper(handler, offset));
    }
}
