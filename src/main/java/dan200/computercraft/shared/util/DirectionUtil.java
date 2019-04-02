/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.util;

import net.minecraft.util.EnumFacing;

public final class DirectionUtil
{
    private DirectionUtil() {}

    public static final EnumFacing[] FACINGS = EnumFacing.values();

    public static EnumFacing toLocal( EnumFacing front, EnumFacing relative )
    {
        if( relative.getAxis() == EnumFacing.Axis.Y ) return relative;

        if( front.getAxis() == EnumFacing.Axis.Y ) front = EnumFacing.NORTH;

        if( relative == front )
        {
            return EnumFacing.SOUTH;
        }
        else if( relative == front.getOpposite() )
        {
            return EnumFacing.NORTH;
        }
        else if( relative == front.rotateYCCW() )
        {
            return EnumFacing.EAST;
        }
        else
        {
            return EnumFacing.WEST;
        }
    }

    public static float toPitchAngle( EnumFacing dir )
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
