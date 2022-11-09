/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.turtle.core;

import dan200.computercraft.api.turtle.ITurtleAccess;
import dan200.computercraft.api.turtle.ITurtleCommand;
import dan200.computercraft.api.turtle.TurtleCommandResult;
import dan200.computercraft.shared.util.WorldUtil;

public class TurtleDetectCommand implements ITurtleCommand {
    private final InteractDirection direction;

    public TurtleDetectCommand(InteractDirection direction) {
        this.direction = direction;
    }

    @Override
    public TurtleCommandResult execute(ITurtleAccess turtle) {
        // Get world direction from direction
        var direction = this.direction.toWorldDir(turtle);

        // Check if thing in front is air or not
        var world = turtle.getLevel();
        var oldPosition = turtle.getPosition();
        var newPosition = oldPosition.relative(direction);

        return !WorldUtil.isLiquidBlock(world, newPosition) && !world.isEmptyBlock(newPosition)
            ? TurtleCommandResult.success()
            : TurtleCommandResult.failure();
    }
}
