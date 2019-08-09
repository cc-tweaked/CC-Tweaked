/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.peripheral.modem.wireless;

import dan200.computercraft.shared.common.BlockGeneric;
import dan200.computercraft.shared.peripheral.modem.ModemShapes;
import dan200.computercraft.shared.util.NamedBlockEntityType;
import dan200.computercraft.shared.util.WaterloggableBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.EntityContext;
import net.minecraft.fluid.FluidState;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.StateFactory;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.IWorld;
import net.minecraft.world.ViewableWorld;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class BlockWirelessModem extends BlockGeneric implements WaterloggableBlock
{
    public static final DirectionProperty FACING = Properties.FACING;
    public static final BooleanProperty ON = BooleanProperty.of( "on" );

    public BlockWirelessModem( Settings settings, NamedBlockEntityType<? extends TileWirelessModem> type )
    {
        super( settings, type );
        setDefaultState( getStateFactory().getDefaultState()
            .with( FACING, Direction.NORTH )
            .with( ON, false )
            .with( WATERLOGGED, false ) );
    }

    @Override
    protected void appendProperties( StateFactory.Builder<Block, BlockState> builder )
    {
        builder.add( FACING, ON, WATERLOGGED );
    }

    @Nonnull
    @Override
    @Deprecated
    public VoxelShape getOutlineShape( BlockState blockState, BlockView world, BlockPos pos, EntityContext position )
    {
        return ModemShapes.getBounds( blockState.get( FACING ) );
    }

    @Nonnull
    @Override
    @Deprecated
    public FluidState getFluidState( BlockState state )
    {
        return getWaterloggedFluidState( state );
    }

    @Nonnull
    @Override
    @Deprecated
    public BlockState getStateForNeighborUpdate( BlockState state, Direction side, BlockState otherState, IWorld world, BlockPos pos, BlockPos otherPos )
    {
        updateWaterloggedPostPlacement( state, world, pos );
        return side == state.get( FACING ) && !state.canPlaceAt( world, pos )
            ? state.getFluidState().getBlockState()
            : state;
    }

    @Override
    @Deprecated
    public boolean canPlaceAt( BlockState state, ViewableWorld world, BlockPos pos )
    {
        Direction facing = state.get( FACING );
        BlockPos offsetPos = pos.offset( facing );
        BlockState offsetState = world.getBlockState( offsetPos );
        return Block.isFaceFullSquare( offsetState.getCollisionShape( world, offsetPos ), facing.getOpposite() );
    }

    @Nullable
    @Override
    public BlockState getPlacementState( ItemPlacementContext placement )
    {
        return getDefaultState()
            .with( FACING, placement.getSide().getOpposite() )
            .with( WATERLOGGED, getWaterloggedStateForPlacement( placement ) );
    }
}
