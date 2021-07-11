/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.peripheral.modem.wired;

import com.google.common.collect.ImmutableMap;
import dan200.computercraft.shared.peripheral.modem.ModemShapes;
import dan200.computercraft.shared.util.DirectionUtil;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;

import java.util.EnumMap;

import static dan200.computercraft.shared.peripheral.modem.wired.BlockCable.*;

public final class CableShapes
{
    private static final double MIN = 0.375;
    private static final double MAX = 1 - MIN;

    private static final VoxelShape SHAPE_CABLE_CORE = VoxelShapes.cuboid( MIN, MIN, MIN, MAX, MAX, MAX );
    private static final EnumMap<Direction, VoxelShape> SHAPE_CABLE_ARM =
        new EnumMap<>( new ImmutableMap.Builder<Direction, VoxelShape>().put( Direction.DOWN,
            VoxelShapes.cuboid(
                MIN,
                0,
                MIN,
                MAX,
                MIN,
                MAX ) )
            .put( Direction.UP,
                VoxelShapes.cuboid(
                    MIN,
                    MAX,
                    MIN,
                    MAX,
                    1,
                    MAX ) )
            .put( Direction.NORTH,
                VoxelShapes.cuboid(
                    MIN,
                    MIN,
                    0,
                    MAX,
                    MAX,
                    MIN ) )
            .put( Direction.SOUTH,
                VoxelShapes.cuboid(
                    MIN,
                    MIN,
                    MAX,
                    MAX,
                    MAX,
                    1 ) )
            .put( Direction.WEST,
                VoxelShapes.cuboid(
                    0,
                    MIN,
                    MIN,
                    MIN,
                    MAX,
                    MAX ) )
            .put( Direction.EAST,
                VoxelShapes.cuboid(
                    MAX,
                    MIN,
                    MIN,
                    1,
                    MAX,
                    MAX ) )
            .build() );

    private static final VoxelShape[] SHAPES = new VoxelShape[(1 << 6) * 7];
    private static final VoxelShape[] CABLE_SHAPES = new VoxelShape[1 << 6];

    private CableShapes()
    {
    }

    public static VoxelShape getCableShape( BlockState state )
    {
        if( !state.get( CABLE ) )
        {
            return VoxelShapes.empty();
        }
        return getCableShape( getCableIndex( state ) );
    }

    private static VoxelShape getCableShape( int index )
    {
        VoxelShape shape = CABLE_SHAPES[index];
        if( shape != null )
        {
            return shape;
        }

        shape = SHAPE_CABLE_CORE;
        for( Direction facing : DirectionUtil.FACINGS )
        {
            if( (index & (1 << facing.ordinal())) != 0 )
            {
                shape = VoxelShapes.union( shape, SHAPE_CABLE_ARM.get( facing ) );
            }
        }

        return CABLE_SHAPES[index] = shape;
    }

    private static int getCableIndex( BlockState state )
    {
        int index = 0;
        for( Direction facing : DirectionUtil.FACINGS )
        {
            if( state.get( CONNECTIONS.get( facing ) ) )
            {
                index |= 1 << facing.ordinal();
            }
        }

        return index;
    }

    public static VoxelShape getShape( BlockState state )
    {
        Direction facing = state.get( MODEM )
            .getFacing();
        if( !state.get( CABLE ) )
        {
            return getModemShape( state );
        }

        int cableIndex = getCableIndex( state );
        int index = cableIndex + ((facing == null ? 0 : facing.ordinal() + 1) << 6);

        VoxelShape shape = SHAPES[index];
        if( shape != null )
        {
            return shape;
        }

        shape = getCableShape( cableIndex );
        if( facing != null )
        {
            shape = VoxelShapes.union( shape, ModemShapes.getBounds( facing ) );
        }
        return SHAPES[index] = shape;
    }

    public static VoxelShape getModemShape( BlockState state )
    {
        Direction facing = state.get( MODEM )
            .getFacing();
        return facing == null ? VoxelShapes.empty() : ModemShapes.getBounds( facing );
    }
}
