/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.util;

import dan200.computercraft.core.computer.ComputerSide;
import dan200.computercraft.shared.common.IDirectionalTile;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.EnumFacing;

public final class DirectionUtil
{
    private DirectionUtil() {}

    public static ComputerSide toLocal( IDirectionalTile directional, EnumFacing dir )
    {
        EnumFacing front = directional.getDirection();
        if( front.getAxis() == EnumFacing.Axis.Y ) front = EnumFacing.NORTH;

        if( dir == front ) return ComputerSide.FRONT;
        if( dir == front.getOpposite() ) return ComputerSide.BACK;
        if( dir == front.rotateYCCW() ) return ComputerSide.LEFT;
        if( dir == front.rotateY() ) return ComputerSide.RIGHT;
        if( dir == EnumFacing.UP ) return ComputerSide.TOP;
        return ComputerSide.BOTTOM;
    }

    public static EnumFacing fromEntityRot( EntityLivingBase player )
    {
        return EnumFacing.fromAngle( player.rotationYaw ).getOpposite();
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

    @Deprecated
    public static float toYawAngle( EnumFacing dir )
    {
        return dir.getHorizontalAngle();
    }
}
