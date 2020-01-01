/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.peripheral.modem.wired;

import com.google.common.collect.ImmutableMap;
import dan200.computercraft.shared.peripheral.modem.ModemBounds;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;

import java.util.EnumMap;
import java.util.List;

public final class CableBounds
{
    public static final double MIN = 0.375;
    public static final double MAX = 1 - MIN;

    private static final AxisAlignedBB SHAPE_CABLE_CORE = new AxisAlignedBB( MIN, MIN, MIN, MAX, MAX, MAX );
    private static final EnumMap<EnumFacing, AxisAlignedBB> SHAPE_CABLE_ARM =
        new EnumMap<>( new ImmutableMap.Builder<EnumFacing, AxisAlignedBB>()
            .put( EnumFacing.DOWN, new AxisAlignedBB( MIN, 0, MIN, MAX, MIN, MAX ) )
            .put( EnumFacing.UP, new AxisAlignedBB( MIN, MAX, MIN, MAX, 1, MAX ) )
            .put( EnumFacing.NORTH, new AxisAlignedBB( MIN, MIN, 0, MAX, MAX, MIN ) )
            .put( EnumFacing.SOUTH, new AxisAlignedBB( MIN, MIN, MAX, MAX, MAX, 1 ) )
            .put( EnumFacing.WEST, new AxisAlignedBB( 0, MIN, MIN, MIN, MAX, MAX ) )
            .put( EnumFacing.EAST, new AxisAlignedBB( MAX, MIN, MIN, 1, MAX, MAX ) )
            .build()
        );

    private static final AxisAlignedBB[] SHAPES = new AxisAlignedBB[(1 << 6) * 7];
    private static final AxisAlignedBB[] CABLE_SHAPES = new AxisAlignedBB[1 << 6];

    private CableBounds()
    {
    }

    private static int getCableIndex( IBlockState state )
    {
        int index = 0;
        for( EnumFacing facing : EnumFacing.VALUES )
        {
            if( state.getValue( BlockCable.CONNECTIONS.get( facing ) ) ) index |= 1 << facing.ordinal();
        }

        return index;
    }

    private static AxisAlignedBB getCableBounds( int index )
    {
        AxisAlignedBB bounds = CABLE_SHAPES[index];
        if( bounds != null ) return bounds;

        bounds = SHAPE_CABLE_CORE;
        for( EnumFacing facing : EnumFacing.VALUES )
        {
            if( (index & (1 << facing.ordinal())) != 0 )
            {
                bounds = bounds.union( SHAPE_CABLE_ARM.get( facing ) );
            }
        }

        return CABLE_SHAPES[index] = bounds;
    }

    public static AxisAlignedBB getCableBounds( IBlockState state )
    {
        if( !state.getValue( BlockCable.CABLE ) ) return BlockCable.NULL_AABB;
        return getCableBounds( getCableIndex( state ) );
    }

    public static void getCableBounds( IBlockState state, List<AxisAlignedBB> bounds )
    {
        if( !state.getValue( BlockCable.CABLE ) ) return;

        bounds.add( SHAPE_CABLE_CORE );
        for( EnumFacing facing : EnumFacing.VALUES )
        {
            if( state.getValue( BlockCable.CONNECTIONS.get( facing ) ) ) bounds.add( SHAPE_CABLE_ARM.get( facing ) );
        }
    }

    public static AxisAlignedBB getModemBounds( IBlockState state )
    {
        EnumFacing facing = state.getValue( BlockCable.MODEM ).getFacing();
        return facing == null ? Block.NULL_AABB : ModemBounds.getBounds( facing );
    }

    public static AxisAlignedBB getBounds( IBlockState state )
    {
        if( !state.getValue( BlockCable.CABLE ) ) return getModemBounds( state );

        EnumFacing facing = state.getValue( BlockCable.MODEM ).getFacing();
        int cableIndex = getCableIndex( state );
        int index = cableIndex + ((facing == null ? 0 : facing.ordinal() + 1) << 6);

        AxisAlignedBB shape = SHAPES[index];
        if( shape != null ) return shape;

        shape = getCableBounds( cableIndex );
        if( facing != null ) shape = shape.union( ModemBounds.getBounds( facing ) );
        return SHAPES[index] = shape;
    }

    public static void getBounds( IBlockState state, List<AxisAlignedBB> bounds )
    {
        EnumFacing facing = state.getValue( BlockCable.MODEM ).getFacing();
        if( facing != null ) bounds.add( getModemBounds( state ) );
        getCableBounds( state, bounds );
    }
}
