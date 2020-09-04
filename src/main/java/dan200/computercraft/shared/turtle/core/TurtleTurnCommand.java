/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.turtle.core;

import javax.annotation.Nonnull;

import dan200.computercraft.api.turtle.ITurtleAccess;
import dan200.computercraft.api.turtle.ITurtleCommand;
import dan200.computercraft.api.turtle.TurtleAnimation;
import dan200.computercraft.api.turtle.TurtleCommandResult;
import dan200.computercraft.api.turtle.event.TurtleAction;
import dan200.computercraft.api.turtle.event.TurtleActionEvent;
import dan200.computercraft.api.turtle.event.TurtleEvent;

public class TurtleTurnCommand implements ITurtleCommand {
    private final TurnDirection m_direction;

    public TurtleTurnCommand(TurnDirection direction) {
        this.m_direction = direction;
    }

    @Nonnull
    @Override
    public TurtleCommandResult execute(@Nonnull ITurtleAccess turtle) {
        TurtleActionEvent event = new TurtleActionEvent(turtle, TurtleAction.TURN);
        if (TurtleEvent.post(event)) {
            return TurtleCommandResult.failure(event.getFailureMessage());
        }

        switch (this.m_direction) {
        case LEFT: {
            turtle.setDirection(turtle.getDirection()
                                      .rotateYCounterclockwise());
            turtle.playAnimation(TurtleAnimation.TURN_LEFT);
            return TurtleCommandResult.success();
        }
        case RIGHT: {
            turtle.setDirection(turtle.getDirection()
                                      .rotateYClockwise());
            turtle.playAnimation(TurtleAnimation.TURN_RIGHT);
            return TurtleCommandResult.success();
        }
        default: {
            return TurtleCommandResult.failure("Unknown direction");
        }
        }
    }
}
