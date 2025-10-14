// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.platform;

import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

public class ForgeContainerTransfer implements ContainerTransfer.Slotted {
    private final ResourceHandler<ItemResource> handler;
    private final int offset;
    private final int limit;
    private final int slots;

    public ForgeContainerTransfer(ResourceHandler<ItemResource> handler) {
        this(handler, 0, handler.size(), handler.size());
    }

    public ForgeContainerTransfer(ResourceHandler<ItemResource> handler, int offset, int limit, int slots) {
        this.handler = handler;
        this.offset = offset;
        this.limit = limit;
        this.slots = slots;
    }

    private int mapSlot(int slot) {
        if (slot < 0 || slot >= limit) throw new IllegalArgumentException("slot is out of bounds");

        slot += offset;
        if (slot >= slots) slot -= limit;
        return slot;
    }

    @Override
    public ForgeContainerTransfer rotate(int offset) {
        return offset == 0 ? this : new ForgeContainerTransfer(handler, mapSlot(offset), limit, slots);
    }

    @Override
    public ForgeContainerTransfer singleSlot(int slot) {
        return slot == 0 && limit == 1 ? this : new ForgeContainerTransfer(handler, mapSlot(slot), 1, slots);
    }

    @Override
    public int moveTo(ContainerTransfer destination, int maxAmount) {
        return moveItem(this, (ForgeContainerTransfer) destination, maxAmount);
    }

    public static int moveItem(ForgeContainerTransfer src, ForgeContainerTransfer dest, int maxAmount) {
        var hasItem = false;

        for (var srcSlot = 0; srcSlot < src.limit; srcSlot++) {
            var actualSrcSlot = src.mapSlot(srcSlot);
            var resource = src.handler.getResource(actualSrcSlot);
            if (resource.isEmpty()) continue;

            // Check how much can be extracted and inserted.
            int maxExtracted;
            try (var transaction = Transaction.openRoot()) {
                maxExtracted = src.handler.extract(actualSrcSlot, resource, maxAmount, transaction);
            }
            if (maxExtracted == 0) continue;

            hasItem = true;

            try (var transaction = Transaction.openRoot()) {
                // check how much can be inserted
                var accepted = dest.insert(resource, maxExtracted, transaction);
                if (accepted == 0) continue;

                // Extract or rollback.
                if (src.handler.extract(actualSrcSlot, resource, accepted, transaction) == accepted) {
                    transaction.commit();
                    return accepted;
                }
            }

        }

        return hasItem ? NO_SPACE : NO_ITEMS;
    }

    private int insert(ItemResource item, int amount, Transaction transaction) {
        var inserted = 0;
        for (var i = 0; i < limit; i++) {
            inserted += handler.insert(mapSlot(i), item, amount - inserted, transaction);
            if (inserted == amount) break;
        }
        return inserted;
    }
}
