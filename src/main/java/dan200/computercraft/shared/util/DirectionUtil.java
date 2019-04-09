/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.util;

import dan200.computercraft.core.computer.ComputerSide;
import net.minecraft.util.EnumFacing;

public final class DirectionUtil
{
    private DirectionUtil() {}

    public static final EnumFacing[] FACINGS = EnumFacing.values();

    public static ComputerSide toLocal( EnumFacing front, EnumFacing dir )
    {
        if( front.getAxis() == EnumFacing.Axis.Y ) front = EnumFacing.NORTH;

        if( dir == front ) return ComputerSide.FRONT;
        if( dir == front.getOpposite() ) return ComputerSide.BACK;
        if( dir == front.rotateYCCW() ) return ComputerSide.LEFT;
        if( dir == front.rotateY() ) return ComputerSide.RIGHT;
        if( dir == EnumFacing.UP ) return ComputerSide.TOP;
        return ComputerSide.BOTTOM;
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
