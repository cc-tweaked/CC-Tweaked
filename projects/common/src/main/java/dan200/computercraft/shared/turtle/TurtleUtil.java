// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.turtle;

import dan200.computercraft.api.turtle.ITurtleAccess;
import dan200.computercraft.shared.platform.ContainerTransfer;
import dan200.computercraft.shared.platform.PlatformHelper;
import dan200.computercraft.shared.util.DropConsumer;
import dan200.computercraft.shared.util.InventoryUtil;
import dan200.computercraft.shared.util.WorldUtil;
import net.minecraft.world.item.ItemStack;

import java.util.function.Function;

public class TurtleUtil {
    /**
     * Get a view of the turtle's inventory starting at the currently selected slot. This should be used when
     * transferring items in to the turtle.
     *
     * @param turtle The turtle to transfer items into.
     * @return The container transfer
     */
    public static ContainerTransfer getOffsetInventory(ITurtleAccess turtle) {
        return PlatformHelper.get().wrapContainer(turtle.getInventory()).rotate(turtle.getSelectedSlot());
    }

    /**
     * Get a view of the turtle's currently selected slot. This should be used when transferring items from the turtle.
     *
     * @param turtle The turtle to transfer items from.
     * @return The container transfer.
     */
    public static ContainerTransfer getSelectedSlot(ITurtleAccess turtle) {
        return PlatformHelper.get().wrapContainer(turtle.getInventory()).singleSlot(turtle.getSelectedSlot());
    }

    /**
     * Store an item in this turtle, or drop it if there is room remaining.
     *
     * @param turtle The turtle to store items into.
     * @param stack  The stack to store.
     */
    public static void storeItemOrDrop(ITurtleAccess turtle, ItemStack stack) {
        if (stack.isEmpty()) return;
        if (turtle.isRemoved()) {
            WorldUtil.dropItemStack(turtle.getLevel(), turtle.getPosition(), null, stack);
            return;
        }

        // Put the remainder back in the turtle
        var remainder = InventoryUtil.storeItemsFromOffset(turtle.getInventory(), stack, turtle.getSelectedSlot());
        if (remainder.isEmpty()) return;

        WorldUtil.dropItemStack(turtle.getLevel(), turtle.getPosition(), turtle.getDirection().getOpposite(), remainder);
    }

    public static Function<ItemStack, ItemStack> dropConsumer(ITurtleAccess turtle) {
        return stack -> turtle.isRemoved() ? stack : InventoryUtil.storeItemsFromOffset(turtle.getInventory(), stack, turtle.getSelectedSlot());
    }

    public static void stopConsuming(ITurtleAccess turtle) {
        var direction = turtle.isRemoved() ? null : turtle.getDirection().getOpposite();
        DropConsumer.clearAndDrop(turtle.getLevel(), turtle.getPosition(), direction);
    }
}
