// SPDX-FileCopyrightText: 2026 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.platform;

import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.SlottedStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;

import java.util.Iterator;
import java.util.NoSuchElementException;

abstract class BasicSlottedStorage implements SlottedStorage<ItemVariant> {
    @Override
    public final long insert(ItemVariant resource, long maxAmount, TransactionContext transaction) {
        long amount = 0;

        for (int i = 0, size = getSlotCount(); i < size; i++) {
            amount += getSlot(i).insert(resource, maxAmount - amount, transaction);
            if (amount == maxAmount) break;
        }

        return amount;
    }

    @Override
    public final long extract(ItemVariant resource, long maxAmount, TransactionContext transaction) {
        long amount = 0;

        for (int i = 0, size = getSlotCount(); i < size; i++) {
            amount += getSlot(i).extract(resource, maxAmount - amount, transaction);
            if (amount == maxAmount) break;
        }

        return amount;
    }

    @Override
    public final Iterator<StorageView<ItemVariant>> iterator() {
        return new Iterator<>() {
            int i = 0;

            @Override
            public boolean hasNext() {
                return i < getSlotCount();
            }

            @Override
            public StorageView<ItemVariant> next() {
                var slot = i++;
                if (slot >= getSlotCount()) throw new NoSuchElementException();
                return getSlot(slot);
            }
        };
    }
}
