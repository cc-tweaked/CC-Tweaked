// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.turtle;

import dan200.computercraft.api.turtle.ITurtleAccess;
import dan200.computercraft.shared.platform.ContainerTransfer;
import dan200.computercraft.shared.platform.PlatformHelper;
import dan200.computercraft.shared.turtle.core.TurtlePlayer;
import dan200.computercraft.shared.util.DropConsumer;
import dan200.computercraft.shared.util.InventoryUtil;
import dan200.computercraft.shared.util.WorldUtil;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;

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
        storeItemOrDrop(turtle, turtle.getInventory(), stack);
    }

    private static void storeItemOrDrop(ITurtleAccess turtle, Container container, ItemStack stack) {
        if (stack.isEmpty()) return;
        if (turtle.isRemoved()) {
            WorldUtil.dropItemStack(turtle.getLevel(), turtle.getPosition(), null, stack);
            return;
        }

        // Put the remainder back in the turtle
        var remainder = InventoryUtil.storeItemsFromOffset(container, stack, turtle.getSelectedSlot());
        if (remainder.isEmpty()) return;

        WorldUtil.dropItemStack(turtle.getLevel(), turtle.getPosition(), turtle.getDirection().getOpposite(), remainder);
    }

    /**
     * Stop a {@link DropConsumer}, and sync the items back to the inventory.
     *
     * @param turtle The turtle to store drops to.
     */
    public static void stopConsuming(ITurtleAccess turtle) {
        for (var stack : DropConsumer.stop()) storeItemOrDrop(turtle, stack);
    }

    /**
     * Stop a {@link DropConsumer}, and sync the items back to the {@link TurtlePlayer} inventory.
     * <p>
     * When using {@link TurtlePlayer#loadInventory(ITurtleAccess)}/{@link TurtlePlayer#unloadInventory(ITurtleAccess)},
     * changes to the turtle's inventory are overridden. This means items must be stored to the <em>player's</em>
     * inventory, not the turtle's.
     *
     * @param turtle The turtle performing this action.
     * @param player The turtle player to store items back to.
     */
    public static void stopConsumingPlayer(ITurtleAccess turtle, TurtlePlayer player) {
        for (var stack : DropConsumer.stop()) storeItemOrDrop(turtle, player.player().getInventory(), stack);
    }
}
