// Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
//
// SPDX-License-Identifier: LicenseRef-CCPL

package dan200.computercraft.shared.turtle.core;

import dan200.computercraft.api.turtle.ITurtleAccess;
import dan200.computercraft.api.turtle.TurtleAnimation;
import dan200.computercraft.api.turtle.TurtleCommand;
import dan200.computercraft.api.turtle.TurtleCommandResult;
import dan200.computercraft.impl.TurtleRefuelHandlers;

public class TurtleRefuelCommand implements TurtleCommand {
    private final int limit;

    public TurtleRefuelCommand(int limit) {
        this.limit = limit;
    }

    @Override
    public TurtleCommandResult execute(ITurtleAccess turtle) {
        var slot = turtle.getSelectedSlot();
        var stack = turtle.getInventory().getItem(slot);
        if (stack.isEmpty()) return TurtleCommandResult.failure("No items to combust");

        var refuelled = TurtleRefuelHandlers.refuel(turtle, stack, slot, limit);
        if (refuelled.isEmpty()) return TurtleCommandResult.failure("Items not combustible");

        var newFuel = refuelled.getAsInt();
        if (newFuel != 0) {
            turtle.addFuel(newFuel);
            turtle.playAnimation(TurtleAnimation.WAIT);
        }

        return TurtleCommandResult.success();
    }
}
