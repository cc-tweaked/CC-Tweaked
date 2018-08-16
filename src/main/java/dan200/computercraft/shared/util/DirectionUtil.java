/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2017. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.util;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.MathHelper;

public class DirectionUtil
{
    public static EnumFacing rotateRight( EnumFacing dir )
    {
        if( dir.getAxis() != EnumFacing.Axis.Y )
        {
            return dir.rotateY();
        }
        else
        {
            return dir;
        }
    }

    public static EnumFacing rotateLeft( EnumFacing dir )
    {
        if( dir.getAxis() != EnumFacing.Axis.Y )
        {
            return dir.rotateYCCW();
        }
        else
        {
            return dir;
        }
    }

    public static EnumFacing rotate180( EnumFacing dir )
    {
        if( dir.getAxis() != EnumFacing.Axis.Y )
        {
            return dir.getOpposite();
        }
        else
        {
            return dir;
        }
    }

    public static int toLocal( EnumFacing front, EnumFacing dir )
    {
        if( front.getAxis() == EnumFacing.Axis.Y )
        {
            front = EnumFacing.NORTH;
        }

        EnumFacing back = rotate180( front );
        EnumFacing left = rotateLeft( front );
        EnumFacing right = rotateRight( front );
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
        int rot = MathHelper.floor( ( player.rotationYaw / 90.0f ) + 0.5f ) & 0x3;
        switch( rot ) {
            case 0: return EnumFacing.NORTH;
            case 1: return EnumFacing.EAST;
            case 2: return EnumFacing.SOUTH;
            case 3: return EnumFacing.WEST;
        }
        return EnumFacing.NORTH;
    }

    public static float toYawAngle( EnumFacing dir )
    {
        switch( dir )
        {
            case NORTH: return 180.0f;
            case SOUTH: return 0.0f;
            case WEST: return 90.0f;
            case EAST: return 270.0f;
            default: return 0.0f;
        }
    }

    public static float toPitchAngle( EnumFacing dir )
    {
        switch( dir )
        {
            case DOWN: return 90.0f;
            case UP: return 270.0f;
            default: return 0.0f;
        }
    }

    public static EnumFacing fromPitchAngle( EntityLivingBase entity )
    {
        if( entity.rotationPitch > 66.5F ) return EnumFacing.UP;
        if( entity.rotationPitch < -66.5F ) return EnumFacing.DOWN;
        return EnumFacing.NORTH;
    }
}
