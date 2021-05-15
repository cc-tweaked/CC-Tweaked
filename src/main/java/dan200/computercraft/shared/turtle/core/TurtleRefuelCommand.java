/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.turtle.core;

import javax.annotation.Nonnull;

import dan200.computercraft.api.turtle.ITurtleAccess;
import dan200.computercraft.api.turtle.ITurtleCommand;
import dan200.computercraft.api.turtle.TurtleAnimation;
import dan200.computercraft.api.turtle.TurtleCommandResult;
import dan200.computercraft.api.turtle.event.TurtleEvent;
import dan200.computercraft.api.turtle.event.TurtleRefuelEvent;

import net.minecraft.item.ItemStack;

public class TurtleRefuelCommand implements ITurtleCommand {
    private final int limit;

    public TurtleRefuelCommand(int limit) {
        this.limit = limit;
    }

    @Nonnull
    @Override
    public TurtleCommandResult execute(@Nonnull ITurtleAccess turtle) {
        int slot = turtle.getSelectedSlot();
        ItemStack stack = turtle.getInventory()
                                .getStack(slot);
        if (stack.isEmpty()) {
            return TurtleCommandResult.failure("No items to combust");
        }

        TurtleRefuelEvent event = new TurtleRefuelEvent(turtle, stack);
        if (TurtleEvent.post(event)) {
            return TurtleCommandResult.failure(event.getFailureMessage());
        }
        if (event.getHandler() == null) {
            return TurtleCommandResult.failure("Items not combustible");
        }

        if (this.limit != 0) {
            turtle.addFuel(event.getHandler()
                                .refuel(turtle, stack, slot, this.limit));
            turtle.playAnimation(TurtleAnimation.WAIT);
        }

        return TurtleCommandResult.success();
    }
}
