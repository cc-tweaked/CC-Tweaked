// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.platform;

import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;

public class ForgeContainerTransfer implements ContainerTransfer.Slotted {
    private final IItemHandler handler;
    private final int offset;
    private final int limit;
    private final int slots;

    public ForgeContainerTransfer(IItemHandler handler) {
        this(handler, 0, handler.getSlots(), handler.getSlots());
    }

    public ForgeContainerTransfer(IItemHandler handler, int offset, int limit, int slots) {
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
        var targetSlot = 0;

        var movedStack = ItemStack.EMPTY;
        var moved = 0;

        outer:
        for (var srcSlot = 0; srcSlot < src.limit; srcSlot++) {
            var actualSrcSlot = src.mapSlot(srcSlot);
            var stack = src.handler.extractItem(actualSrcSlot, maxAmount, true);
            if (stack.isEmpty()) continue;

            // Pick the first item in the inventory to be the one we transfer, skipping those that match.
            if (movedStack.isEmpty()) {
                movedStack = stack.copy();
                if (stack.getMaxStackSize() < maxAmount) maxAmount = stack.getMaxStackSize();
            } else if (!ItemStack.isSameItemSameTags(stack, movedStack)) {
                continue;
            }

            for (; targetSlot < dest.limit; targetSlot++) {
                var oldCount = stack.getCount();
                stack = dest.handler.insertItem(dest.mapSlot(targetSlot), stack, false);

                var transferred = oldCount - stack.getCount();
                var extracted = src.handler.extractItem(actualSrcSlot, transferred, false);

                moved += transferred;

                // We failed to extract as much as we should have. This should never happen, but goodness knows.
                if (extracted.getCount() < transferred) break outer;

                if (moved >= maxAmount) return moved;
                if (stack.isEmpty()) break;
            }
        }

        if (moved == 0) return movedStack.isEmpty() ? NO_ITEMS : NO_SPACE;
        return moved;
    }
}
