/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.turtle.core;

import dan200.computercraft.api.turtle.ITurtleAccess;
import net.minecraft.util.EnumFacing;

public enum MoveDirection
{
    Forward,
    Back,
    Up,
    Down;

    public EnumFacing toWorldDir( ITurtleAccess turtle )
    {
        switch( this )
        {
            case Forward:
            default:
                return turtle.getDirection();
            case Back:
                return turtle.getDirection().getOpposite();
            case Up:
                return EnumFacing.UP;
            case Down:
                return EnumFacing.DOWN;
        }
    }
}
