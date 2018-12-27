/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2018. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.peripheral.modem;

import net.minecraft.block.Block;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;

import javax.annotation.Nonnull;

public class ModemBounds
{
    private static final AxisAlignedBB[] BOXES = new AxisAlignedBB[] {
        new AxisAlignedBB( 0.125, 0.0, 0.125, 0.875, 0.1875, 0.875 ), // Down
        new AxisAlignedBB( 0.125, 0.8125, 0.125, 0.875, 1.0, 0.875 ), // Up
        new AxisAlignedBB( 0.125, 0.125, 0.0, 0.875, 0.875, 0.1875 ), // North
        new AxisAlignedBB( 0.125, 0.125, 0.8125, 0.875, 0.875, 1.0 ), // South
        new AxisAlignedBB( 0.0, 0.125, 0.125, 0.1875, 0.875, 0.875 ), // West
        new AxisAlignedBB( 0.8125, 0.125, 0.125, 1.0, 0.875, 0.875 ), // East
    };

    @Nonnull
    public static AxisAlignedBB getBounds( EnumFacing facing )
    {
        int direction = facing.ordinal();
        return direction < BOXES.length ? BOXES[direction] : Block.FULL_BLOCK_AABB;
    }
}
