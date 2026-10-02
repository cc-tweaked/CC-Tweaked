// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.platform;

import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.SlottedStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageUtil;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleSlotStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

import java.util.Iterator;
import java.util.Objects;

@SuppressWarnings("UnstableApiUsage")
public final class FabricItemContainer implements ItemContainer {
    private final Storage<ItemVariant> storage;
    private final @Nullable SlottedStorage<ItemVariant> slottedStorage;

    public FabricItemContainer(SlottedStorage<ItemVariant> storage) {
        this.storage = this.slottedStorage = storage;
    }

    public FabricItemContainer(Storage<ItemVariant> storage) {
        this.storage = storage;
        this.slottedStorage = storage instanceof SlottedStorage<ItemVariant> slotted ? slotted : null;
    }

    @Override
    public int size() {
        return slottedStorage != null ? slottedStorage.getSlotCount() : Iterables.size(storage);
    }

    @Override
    public Iterator<ItemStack> contents() {
        return Iterators.transform(storage.iterator(), FabricItemContainer::toStack);
    }

    private StorageView<ItemVariant> getSlot(int slot) {
        if (slottedStorage != null) return slottedStorage.getSlot(slot);

        var singleSlot = Iterables.get(storage, slot);
        if (singleSlot == null) throw new IndexOutOfBoundsException();
        return singleSlot;
    }

    @Override
    public ItemStack getItem(int slot) {
        return toStack(getSlot(slot));
    }

    private static ItemStack toStack(StorageView<ItemVariant> variant) {
        if (variant.isResourceBlank() || variant.getAmount() <= 0) return ItemStack.EMPTY;
        return variant.getResource().toStack(saturatingToInt(variant.getAmount()));
    }

    @Override
    public int getCapacity(int slot) {
        return saturatingToInt(getSlot(slot).getCapacity());
    }

    @Override
    public @Nullable ItemContainer singleSlot(int slot) {
        return slottedStorage != null ? new FabricItemContainer(slottedStorage.getSlot(slot)) : null;
    }

    @Override
    public int moveTo(ItemContainer destination, int maxAmount) {
        var hasItems = false;
        var destStorage = ((FabricItemContainer) destination).storage;
        for (var slot : storage.nonEmptyViews()) {
            var result = move(slot, destStorage, maxAmount);
            if (result >= 0) return result;

            if (result == NO_SPACE) hasItems = true;
        }

        return hasItems ? NO_SPACE : NO_ITEMS;
    }

    @Override
    public int moveTo(int fromSlot, ItemContainer destination, int maxAmount) {
        return move(getSlot(fromSlot), ((FabricItemContainer) destination).storage, maxAmount);
    }

    private static int move(StorageView<ItemVariant> from, Storage<ItemVariant> to, long maxAmount) {
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
                return saturatingToInt(accepted);
            }

            return NO_SPACE;
        }
    }

    private static int saturatingToInt(long value) {
        return value > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) value;
    }

    private static final class OffsetStorage extends BasicSlottedStorage {
        private final SlottedStorage<ItemVariant> storage;
        private final int offset;

        private OffsetStorage(SlottedStorage<ItemVariant> storage, int offset) {
            this.storage = storage;
            this.offset = offset;
        }

        @Override
        public boolean supportsInsertion() {
            return storage.supportsInsertion();
        }

        @Override
        public boolean supportsExtraction() {
            return storage.supportsExtraction();
        }

        @Override
        public long getVersion() {
            return storage.getVersion();
        }

        @Override
        public int getSlotCount() {
            return storage.getSlotCount();
        }

        @Override
        public SingleSlotStorage<ItemVariant> getSlot(int slot) {
            var size = getSlotCount();
            Objects.checkIndex(slot, size);

            var actualSlot = slot + offset;
            return storage.getSlot(actualSlot >= size ? actualSlot - size : actualSlot);
        }
    }

    static ItemContainer rotated(SlottedStorage<ItemVariant> storage, int offset) {
        return new FabricItemContainer(new OffsetStorage(storage, offset));
    }
}
