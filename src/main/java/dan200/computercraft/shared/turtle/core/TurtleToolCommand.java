/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.turtle.core;

import dan200.computercraft.api.turtle.*;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Locale;

public class TurtleToolCommand implements ITurtleCommand
{
    private final TurtleVerb verb;
    private final InteractDirection direction;
    private final TurtleSide side;

    public TurtleToolCommand( TurtleVerb verb, InteractDirection direction, TurtleSide side )
    {
        this.verb = verb;
        this.direction = direction;
        this.side = side;
    }

    @Nonnull
    @Override
    public TurtleCommandResult execute( @Nonnull ITurtleAccess turtle )
    {
        TurtleCommandResult firstFailure = null;
        for( TurtleSide side : TurtleSide.values() )
        {
            if( this.side != null && this.side != side ) continue;

            ITurtleUpgrade upgrade = turtle.getUpgrade( side );
            if( upgrade == null || !upgrade.getType().isTool() ) continue;

            TurtleCommandResult result = upgrade.useTool( turtle, side, verb, direction.toWorldDir( turtle ) );
            if( result.isSuccess() )
            {
                switch( side )
                {
                    case LEFT:
                        turtle.playAnimation( TurtleAnimation.SWING_LEFT_TOOL );
                        break;
                    case RIGHT:
                        turtle.playAnimation( TurtleAnimation.SWING_RIGHT_TOOL );
                        break;
                    default:
                        turtle.playAnimation( TurtleAnimation.WAIT );
                        break;
                }
                return result;
            }
            else if( firstFailure == null )
            {
                firstFailure = result;
            }
        }
        return firstFailure != null ? firstFailure
            : TurtleCommandResult.failure( "No tool to " + verb.name().toLowerCase( Locale.ROOT ) + " with" );
    }

    public static TurtleToolCommand attack( InteractDirection direction, @Nullable TurtleSide side )
    {
        return new TurtleToolCommand( TurtleVerb.ATTACK, direction, side );
    }

    public static TurtleToolCommand dig( InteractDirection direction, @Nullable TurtleSide side )
    {
        return new TurtleToolCommand( TurtleVerb.DIG, direction, side );
    }
}
