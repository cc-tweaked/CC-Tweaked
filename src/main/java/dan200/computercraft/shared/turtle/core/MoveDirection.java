/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.turtle.core;

import dan200.computercraft.api.turtle.ITurtleAccess;
import net.minecraft.core.Direction;

public enum MoveDirection
{
    FORWARD,
    BACK,
    UP,
    DOWN;

    public Direction toWorldDir( ITurtleAccess turtle )
    {
        switch( this )
        {
            case FORWARD:
            default:
                return turtle.getDirection();
            case BACK:
                return turtle.getDirection().getOpposite();
            case UP:
                return Direction.UP;
            case DOWN:
                return Direction.DOWN;
        }
    }
}
