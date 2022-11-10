/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.platform;

import net.fabricmc.fabric.api.transfer.v1.item.InventoryStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageUtil;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;

import javax.annotation.Nullable;
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
        return storage instanceof InventoryStorage inv ? new SlottedImpl(inv) : new FabricContainerTransfer(storage);
    }

    public static ContainerTransfer.Slotted of(InventoryStorage storage) {
        return new SlottedImpl(storage);
    }

    @Override
    public int moveTo(ContainerTransfer destination, int maxAmount) {
        var predicate = new GatePredicate<ItemVariant>();

        var moved = StorageUtil.move(storage, ((FabricContainerTransfer) destination).storage, predicate, maxAmount, null);
        if (moved > 0) return moved > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) moved;

        // Nasty hack here to check if move() actually found an item in the original inventory. Saves having to
        // iterate over the source twice.
        return predicate.hasItem() ? NO_SPACE : NO_ITEMS;
    }

    /**
     * A predicate which accepts the first value it sees, and then only those matching that value.
     *
     * @param <T> The type of the object to accept.
     */
    private static class GatePredicate<T> implements Predicate<T> {
        private @Nullable T instance = null;

        @Override
        public boolean test(T o) {
            if (instance == null) {
                instance = o;
                return true;
            }

            return instance.equals(o);
        }

        boolean hasItem() {
            return instance != null;
        }
    }

    private static class SlottedImpl extends FabricContainerTransfer implements ContainerTransfer.Slotted {
        private final InventoryStorage storage;

        SlottedImpl(InventoryStorage storage) {
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

    private record OffsetStorage(InventoryStorage storage, int offset) implements Storage<ItemVariant> {
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
                transferred += slots.get(wrap(i, size)).insert(resource, maxAmount, transaction);
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
                transferred += slots.get(wrap(i, size)).extract(resource, maxAmount, transaction);
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
