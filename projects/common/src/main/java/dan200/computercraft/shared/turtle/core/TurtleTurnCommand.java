// Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
//
// SPDX-License-Identifier: LicenseRef-CCPL

package dan200.computercraft.shared.turtle.core;

import dan200.computercraft.api.turtle.ITurtleAccess;
import dan200.computercraft.api.turtle.TurtleAnimation;
import dan200.computercraft.api.turtle.TurtleCommand;
import dan200.computercraft.api.turtle.TurtleCommandResult;

public class TurtleTurnCommand implements TurtleCommand {
    private final TurnDirection direction;

    public TurtleTurnCommand(TurnDirection direction) {
        this.direction = direction;
    }

    @Override
    public TurtleCommandResult execute(ITurtleAccess turtle) {
        switch (direction) {
            case LEFT -> {
                turtle.setDirection(turtle.getDirection().getCounterClockWise());
                turtle.playAnimation(TurtleAnimation.TURN_LEFT);
                return TurtleCommandResult.success();
            }
            case RIGHT -> {
                turtle.setDirection(turtle.getDirection().getClockWise());
                turtle.playAnimation(TurtleAnimation.TURN_RIGHT);
                return TurtleCommandResult.success();
            }
            default -> {
                return TurtleCommandResult.failure("Unknown direction");
            }
        }
    }
}
