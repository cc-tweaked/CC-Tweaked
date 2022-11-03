/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.turtle;

import dan200.computercraft.api.turtle.ITurtleAccess;
import dan200.computercraft.shared.util.DropConsumer;
import dan200.computercraft.shared.util.InventoryUtil;
import dan200.computercraft.shared.util.WorldUtil;
import net.minecraft.world.item.ItemStack;

import java.util.function.Function;

public class TurtleUtil {
    /**
     * Store an item in this turtle, or drop it if there is room remaining.
     *
     * @param turtle The turtle to store items into.
     * @param stack  The stack to store.
     */
    public static void storeItemOrDrop(ITurtleAccess turtle, ItemStack stack) {
        if (stack.isEmpty()) return;
        if (turtle.isRemoved()) {
            WorldUtil.dropItemStack(stack, turtle.getLevel(), turtle.getPosition(), null);
            return;
        }

        // Put the remainder back in the turtle
        var remainder = InventoryUtil.storeItems(stack, turtle.getItemHandler(), turtle.getSelectedSlot());
        if (remainder.isEmpty()) return;

        WorldUtil.dropItemStack(remainder, turtle.getLevel(), turtle.getPosition(), turtle.getDirection().getOpposite());
    }

    public static Function<ItemStack, ItemStack> dropConsumer(ITurtleAccess turtle) {
        return stack -> turtle.isRemoved() ? stack : InventoryUtil.storeItems(stack, turtle.getItemHandler(), turtle.getSelectedSlot());
    }

    public static void stopConsuming(ITurtleAccess turtle) {
        var direction = turtle.isRemoved() ? null : turtle.getDirection().getOpposite();
        DropConsumer.clearAndDrop(turtle.getLevel(), turtle.getPosition(), direction);
    }
}
