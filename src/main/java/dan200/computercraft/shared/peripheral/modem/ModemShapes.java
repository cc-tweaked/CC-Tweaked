/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.peripheral.modem;

import net.minecraft.core.Direction;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nonnull;

public final class ModemShapes
{
    private static final VoxelShape[] BOXES = new VoxelShape[] {
        Shapes.box( 0.125, 0.0, 0.125, 0.875, 0.1875, 0.875 ), // Down
        Shapes.box( 0.125, 0.8125, 0.125, 0.875, 1.0, 0.875 ), // Up
        Shapes.box( 0.125, 0.125, 0.0, 0.875, 0.875, 0.1875 ), // North
        Shapes.box( 0.125, 0.125, 0.8125, 0.875, 0.875, 1.0 ), // South
        Shapes.box( 0.0, 0.125, 0.125, 0.1875, 0.875, 0.875 ), // West
        Shapes.box( 0.8125, 0.125, 0.125, 1.0, 0.875, 0.875 ), // East
    };

    @Nonnull
    public static VoxelShape getBounds( Direction facing )
    {
        int direction = facing.ordinal();
        return direction < BOXES.length ? BOXES[direction] : Shapes.block();
    }
}
