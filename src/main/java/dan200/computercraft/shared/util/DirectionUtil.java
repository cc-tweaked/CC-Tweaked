/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.util;

import net.minecraft.util.math.Direction;

public final class DirectionUtil
{
    private DirectionUtil() {}

    public static final Direction[] FACINGS = Direction.values();

    public static Direction toLocal( Direction front, Direction relative )
    {
        if( relative.getAxis() == Direction.Axis.Y ) return relative;

        if( front.getAxis() == Direction.Axis.Y ) front = Direction.NORTH;

        if( relative == front )
        {
            return Direction.SOUTH;
        }
        else if( relative == front.getOpposite() )
        {
            return Direction.NORTH;
        }
        else if( relative == front.rotateYCounterclockwise() )
        {
            return Direction.EAST;
        }
        else
        {
            return Direction.WEST;
        }
    }

    public static float toPitchAngle( Direction dir )
    {
        switch( dir )
        {
            case DOWN:
                return 90.0f;
            case UP:
                return 270.0f;
            default:
                return 0.0f;
        }
    }
}
