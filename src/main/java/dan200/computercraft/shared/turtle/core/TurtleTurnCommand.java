/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.turtle.core;

import dan200.computercraft.api.turtle.ITurtleAccess;
import dan200.computercraft.api.turtle.ITurtleCommand;
import dan200.computercraft.api.turtle.TurtleAnimation;
import dan200.computercraft.api.turtle.TurtleCommandResult;
import dan200.computercraft.api.turtle.event.TurtleAction;
import dan200.computercraft.api.turtle.event.TurtleActionEvent;
import net.minecraftforge.common.MinecraftForge;

import javax.annotation.Nonnull;

public class TurtleTurnCommand implements ITurtleCommand
{
    private final TurnDirection direction;

    public TurtleTurnCommand( TurnDirection direction )
    {
        this.direction = direction;
    }

    @Nonnull
    @Override
    public TurtleCommandResult execute( @Nonnull ITurtleAccess turtle )
    {
        TurtleActionEvent event = new TurtleActionEvent( turtle, TurtleAction.TURN );
        if( MinecraftForge.EVENT_BUS.post( event ) )
        {
            return TurtleCommandResult.failure( event.getFailureMessage() );
        }

        switch( direction )
        {
            case LEFT:
            {
                turtle.setDirection( turtle.getDirection().getCounterClockWise() );
                turtle.playAnimation( TurtleAnimation.TURN_LEFT );
                return TurtleCommandResult.success();
            }
            case RIGHT:
            {
                turtle.setDirection( turtle.getDirection().getClockWise() );
                turtle.playAnimation( TurtleAnimation.TURN_RIGHT );
                return TurtleCommandResult.success();
            }
            default:
            {
                return TurtleCommandResult.failure( "Unknown direction" );
            }
        }
    }
}
