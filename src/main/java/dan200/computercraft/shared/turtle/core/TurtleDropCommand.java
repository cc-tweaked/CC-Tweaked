/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.turtle.core;

import dan200.computercraft.api.turtle.ITurtleAccess;
import dan200.computercraft.api.turtle.ITurtleCommand;
import dan200.computercraft.api.turtle.TurtleAnimation;
import dan200.computercraft.api.turtle.TurtleCommandResult;
import dan200.computercraft.shared.util.InventoryUtil;
import dan200.computercraft.shared.util.WorldUtil;

import javax.annotation.Nonnull;

public class TurtleDropCommand implements ITurtleCommand {
    private final InteractDirection direction;
    private final int quantity;

    public TurtleDropCommand(InteractDirection direction, int quantity) {
        this.direction = direction;
        this.quantity = quantity;
    }

    @Nonnull
    @Override
    public TurtleCommandResult execute(@Nonnull ITurtleAccess turtle) {
        // Dropping nothing is easy
        if (quantity == 0) {
            turtle.playAnimation(TurtleAnimation.WAIT);
            return TurtleCommandResult.success();
        }

        // Get world direction from direction
        var direction = this.direction.toWorldDir(turtle);

        // Get things to drop
        var stack = turtle.getInventory().removeItem(turtle.getSelectedSlot(), quantity);
        if (stack.isEmpty()) {
            return TurtleCommandResult.failure("No items to drop");
        }
        turtle.getInventory().setChanged();

        // Get inventory for thing in front
        var world = turtle.getLevel();
        var oldPosition = turtle.getPosition();
        var newPosition = oldPosition.relative(direction);
        var side = direction.getOpposite();

        var inventory = InventoryUtil.getInventory(world, newPosition, side);

        if (inventory != null) {
            // Drop the item into the inventory
            var remainder = InventoryUtil.storeItems(stack, inventory);
            if (!remainder.isEmpty()) {
                // Put the remainder back in the turtle
                InventoryUtil.storeItems(remainder, turtle.getItemHandler(), turtle.getSelectedSlot());
            }

            // Return true if we stored anything
            if (remainder != stack) {
                turtle.playAnimation(TurtleAnimation.WAIT);
                return TurtleCommandResult.success();
            } else {
                return TurtleCommandResult.failure("No space for items");
            }
        } else {
            // Drop the item into the world
            WorldUtil.dropItemStack(stack, world, oldPosition, direction);
            world.globalLevelEvent(1000, newPosition, 0);
            turtle.playAnimation(TurtleAnimation.WAIT);
            return TurtleCommandResult.success();
        }
    }
}
