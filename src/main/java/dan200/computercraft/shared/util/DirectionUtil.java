/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.util;

import dan200.computercraft.shared.common.IDirectionalTile;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.EnumFacing;

public class DirectionUtil
{
    public static int toLocal( IDirectionalTile directional, EnumFacing dir )
    {
        EnumFacing front = directional.getDirection();
        if( front.getAxis() == EnumFacing.Axis.Y ) front = EnumFacing.NORTH;

        EnumFacing back = front.getOpposite();
        EnumFacing left = front.rotateYCCW();
        EnumFacing right = front.rotateY();
        if( dir == front )
        {
            return 3;
        }
        else if( dir == back )
        {
            return 2;
        }
        else if( dir == left )
        {
            return 5;
        }
        else if( dir == right )
        {
            return 4;
        }
        else if( dir == EnumFacing.UP )
        {
            return 1;
        }
        else
        {
            return 0;
        }
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
