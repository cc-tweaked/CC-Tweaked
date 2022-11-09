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

public class TurtleTransferToCommand implements ITurtleCommand {
    private final int slot;
    private final int quantity;

    public TurtleTransferToCommand(int slot, int limit) {
        this.slot = slot;
        quantity = limit;
    }

    @Override
    public TurtleCommandResult execute(ITurtleAccess turtle) {
        // Take stack
        var stack = turtle.getInventory().removeItem(turtle.getSelectedSlot(), quantity);
        if (stack.isEmpty()) {
            turtle.playAnimation(TurtleAnimation.WAIT);
            return TurtleCommandResult.success();
        }

        // Store stack
        var remainder = InventoryUtil.storeItemsIntoSlot(turtle.getInventory(), stack, slot);
        if (!remainder.isEmpty()) {
            // Put the remainder back
            InventoryUtil.storeItemsIntoSlot(turtle.getInventory(), remainder, turtle.getSelectedSlot());
        }

        // Return true if we moved anything
        if (remainder != stack) {
            turtle.playAnimation(TurtleAnimation.WAIT);
            return TurtleCommandResult.success();
        } else {
            return TurtleCommandResult.failure("No space for items");
        }
    }
}
