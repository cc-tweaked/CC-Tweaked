/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.peripheral.modem;

import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;

import javax.annotation.Nonnull;

public class ModemShapes
{
    private static final VoxelShape[] BOXES = new VoxelShape[] {
        VoxelShapes.cube( 0.125, 0.0, 0.125, 0.875, 0.1875, 0.875 ), // Down
        VoxelShapes.cube( 0.125, 0.8125, 0.125, 0.875, 1.0, 0.875 ), // Up
        VoxelShapes.cube( 0.125, 0.125, 0.0, 0.875, 0.875, 0.1875 ), // North
        VoxelShapes.cube( 0.125, 0.125, 0.8125, 0.875, 0.875, 1.0 ), // South
        VoxelShapes.cube( 0.0, 0.125, 0.125, 0.1875, 0.875, 0.875 ), // West
        VoxelShapes.cube( 0.8125, 0.125, 0.125, 1.0, 0.875, 0.875 ), // East
    };

    @Nonnull
    public static VoxelShape getBounds( Direction facing )
    {
        int direction = facing.ordinal();
        return direction < BOXES.length ? BOXES[direction] : VoxelShapes.fullCube();
    }
}
