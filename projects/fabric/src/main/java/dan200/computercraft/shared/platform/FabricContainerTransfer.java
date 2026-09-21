// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.platform;

import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.SlottedStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageUtil;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.function.Predicate;

@SuppressWarnings("UnstableApiUsage")
public class FabricContainerTransfer implements ContainerTransfer {
    private final Storage<ItemVariant> storage;

    private FabricContainerTransfer(Storage<ItemVariant> storage) {
        this.storage = storage;
    }

    public static ContainerTransfer of(Storage<ItemVariant> storage) {
        return storage instanceof SlottedStorage<ItemVariant> inv ? new SlottedImpl(inv) : new FabricContainerTransfer(storage);
    }

    public static ContainerTransfer.Slotted of(SlottedStorage<ItemVariant> storage) {
        return new SlottedImpl(storage);
    }

    /**
     * Move an item from a slot into another storage.
     *
     * @param from      The slot to move from.
     * @param to        The storage to move to.
     * @param maxAmount The max amount to move.
     * @return The number of items which were transferred, or one of {@link #NO_ITEMS} or {@link #NO_SPACE}. This will
     * <em>NEVER</em> return 0.
     * @see StorageUtil#move(Storage, Storage, Predicate, long, TransactionContext)
     * @see #moveTo(ContainerTransfer, int)
     */
    public static int move(StorageView<ItemVariant> from, Storage<ItemVariant> to, long maxAmount) {
        var resource = from.getResource();
        if (resource.isBlank()) return NO_ITEMS;

        try (var transaction = Transaction.openOuter()) {
            // Check how much can be extracted and inserted.
            var maxExtracted = StorageUtil.simulateExtract(from, resource, maxAmount, transaction);
            if (maxExtracted == 0) return NO_ITEMS;

            var accepted = to.insert(resource, maxExtracted, transaction);
            if (accepted == 0) return NO_SPACE;

            // Extract or rollback.
            if (from.extract(resource, accepted, transaction) == accepted) {
                transaction.commit();
                return accepted > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) accepted;
            }

            return NO_SPACE;
        }
    }

    @Override
    public int moveTo(ContainerTransfer destination, int maxAmount) {
        var hasItem = false;
        var destStorage = ((FabricContainerTransfer) destination).storage;
        for (var slot : storage.nonEmptyViews()) {
            var result = move(slot, destStorage, maxAmount);
            if (result >= 0) return result;

            if (result == NO_SPACE) hasItem = true;
        }

        return hasItem ? NO_SPACE : NO_ITEMS;
    }

    private static final class SlottedImpl extends FabricContainerTransfer implements ContainerTransfer.Slotted {
        private final SlottedStorage<ItemVariant> storage;

        SlottedImpl(SlottedStorage<ItemVariant> storage) {
            super(storage);
            this.storage = storage;
        }

        @Override
        public ContainerTransfer rotate(int offset) {
            return offset == 0 ? this : of(new OffsetStorage(storage, offset));
        }

        @Override
        public ContainerTransfer singleSlot(int slot) {
            return of(storage.getSlot(slot));
        }
    }

    private record OffsetStorage(SlottedStorage<ItemVariant> storage, int offset) implements Storage<ItemVariant> {
        @Override
        public boolean supportsInsertion() {
            for (var slot : storage.getSlots()) {
                if (slot.supportsInsertion()) return true;
            }
            return false;
        }

        @Override
        public boolean supportsExtraction() {
            for (var slot : storage.getSlots()) {
                if (slot.supportsExtraction()) return true;
            }
            return false;
        }

        @Override
        public long insert(ItemVariant resource, long maxAmount, TransactionContext transaction) {
            var slots = storage.getSlots();
            var size = slots.size();

            long transferred = 0;
            for (var i = 0; i < size; i++) {
                transferred += slots.get(wrap(i, size)).insert(resource, maxAmount - transferred, transaction);
                if (transferred >= maxAmount) break;
            }

            return transferred;
        }

        @Override
        public long extract(ItemVariant resource, long maxAmount, TransactionContext transaction) {
            var slots = storage.getSlots();
            var size = slots.size();

            long transferred = 0;
            for (var i = 0; i < size; i++) {
                transferred += slots.get(wrap(i, size)).extract(resource, maxAmount - transferred, transaction);
                if (transferred >= maxAmount) break;
            }

            return transferred;
        }

        @Override
        public Iterator<StorageView<ItemVariant>> iterator() {
            var slots = storage.getSlots();
            var size = slots.size();
            return new Iterator<>() {
                int i = 0;

                @Override
                public boolean hasNext() {
                    return i < size;
                }

                @Override
                public StorageView<ItemVariant> next() {
                    var slot = i++;
                    if (slot >= size) throw new NoSuchElementException();
                    return slots.get(wrap(slot, size));
                }
            };
        }

        int wrap(int slot, int size) {
            var actualSlot = slot + offset;
            return actualSlot >= size ? actualSlot - size : actualSlot;
        }
    }
}
