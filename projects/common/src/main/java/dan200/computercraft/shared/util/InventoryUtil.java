// Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
//
// SPDX-License-Identifier: LicenseRef-CCPL

package dan200.computercraft.shared.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

public final class InventoryUtil {
    private InventoryUtil() {
    }

    /**
     * Get the inventory slot for a given hand.
     *
     * @param player The player to get the slot from.
     * @param hand   The hand to get.
     * @return The current slot.
     */
    public static int getHandSlot(Player player, InteractionHand hand) {
        return switch (hand) {
            case MAIN_HAND -> player.getInventory().selected;
            case OFF_HAND -> Inventory.SLOT_OFFHAND;
        };
    }

    public static @Nullable Container getEntityContainer(ServerLevel level, BlockPos pos, Direction side) {
        var vecStart = new Vec3(
            pos.getX() + 0.5 + 0.6 * side.getStepX(),
            pos.getY() + 0.5 + 0.6 * side.getStepY(),
            pos.getZ() + 0.5 + 0.6 * side.getStepZ()
        );

        var dir = side.getOpposite();
        var vecDir = new Vec3(dir.getStepX(), dir.getStepY(), dir.getStepZ());

        var hit = WorldUtil.clip(level, vecStart, vecDir, 1.1, null);
        return hit instanceof EntityHitResult entityHit && entityHit.getEntity() instanceof Container container ? container : null;
    }

    // Methods for placing into inventories:

    public static ItemStack storeItemsIntoSlot(Container container, ItemStack stack, int slot) {
        return storeItems(container, stack, slot, 1);
    }

    public static ItemStack storeItemsFromOffset(Container container, ItemStack stack, int offset) {
        return storeItems(container, stack, offset, container.getContainerSize());
    }

    private static ItemStack storeItems(Container container, ItemStack stack, int offset, int slotCount) {
        var originalCount = stack.getCount();
        var remainder = storeItemsImpl(container, stack, offset, slotCount);
        if (remainder.getCount() != originalCount) container.setChanged();
        return remainder;
    }

    private static ItemStack storeItemsImpl(Container container, ItemStack stack, int offset, int slotCount) {
        var limit = container.getContainerSize();
        var maxSize = Math.min(stack.getMaxStackSize(), container.getMaxStackSize());
        if (maxSize <= 0) return stack;

        for (var i = 0; i < slotCount; i++) {
            var slot = i + offset;
            if (slot >= limit) slot -= limit;
            var currentStack = container.getItem(slot);
            if (currentStack.isEmpty()) {
                // If the current slot is empty and we can place them item then there's two cases:
                if (!container.canPlaceItem(slot, stack)) continue;

                if (stack.getCount() <= maxSize) {
                    // If there's room to put the item in directly, do so.
                    container.setItem(slot, stack);
                    return ItemStack.EMPTY;
                } else {
                    // Otherwise, take maxSize items from the stack and continue on our loop.
                    container.setItem(slot, stack.split(maxSize));
                }
            } else {
                // If the current slot is non-empty, we've got space, and there's compatible items then:
                if (currentStack.getCount() >= Math.min(currentStack.getMaxStackSize(), maxSize)) continue;
                if (!canMergeItems(currentStack, stack)) continue;

                // Determine how much space we have, and thus how much we can move - then move it!
                var toMove = Math.min(stack.getCount(), maxSize - currentStack.getCount());
                currentStack.grow(toMove);
                stack.shrink(toMove);
                if (stack.isEmpty()) return ItemStack.EMPTY;
            }
        }

        return stack;
    }

    private static boolean canMergeItems(ItemStack stack1, ItemStack stack2) {
        if (stack1.getItem() != stack2.getItem()) return false;
        if (stack1.getDamageValue() != stack2.getDamageValue()) return false;
        if (stack1.getCount() > stack1.getMaxStackSize()) return false;
        return ItemStack.isSameItemSameTags(stack1, stack2);
    }
}
