/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.peripheral.modem.wireless;

import dan200.computercraft.shared.common.BlockGeneric;
import dan200.computercraft.shared.peripheral.modem.ModemShapes;
import dan200.computercraft.shared.util.WaterloggableBlock;
import net.minecraft.block.Block;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.IBlockState;
import net.minecraft.fluid.IFluidState;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.state.BooleanProperty;
import net.minecraft.state.DirectionProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.IWorld;
import net.minecraft.world.IWorldReaderBase;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class BlockWirelessModem extends BlockGeneric implements WaterloggableBlock
{
    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    public static final BooleanProperty ON = BooleanProperty.create( "on" );

    public BlockWirelessModem( Properties settings, TileEntityType<? extends TileWirelessModem> type )
    {
        super( settings, type );
        setDefaultState( getStateContainer().getBaseState()
            .with( FACING, EnumFacing.NORTH )
            .with( ON, false )
            .with( WATERLOGGED, false ) );
    }

    @Override
    protected void fillStateContainer( StateContainer.Builder<Block, IBlockState> builder )
    {
        builder.add( FACING, ON, WATERLOGGED );
    }

    @Nonnull
    @Override
    @Deprecated
    public VoxelShape getShape( IBlockState blockState, IBlockReader blockView, BlockPos blockPos )
    {
        return ModemShapes.getBounds( blockState.get( FACING ) );
    }

    @Nonnull
    @Override
    @Deprecated
    public IFluidState getFluidState( IBlockState state )
    {
        return getWaterloggedFluidState( state );
    }

    @Nonnull
    @Override
    @Deprecated
    public IBlockState updatePostPlacement( @Nonnull IBlockState state, EnumFacing side, IBlockState otherState, IWorld world, BlockPos pos, BlockPos otherPos )
    {
        updateWaterloggedPostPlacement( state, world, pos );
        return side == state.get( FACING ) && !state.isValidPosition( world, pos )
            ? state.getFluidState().getBlockState()
            : state;
    }

    @Override
    @Deprecated
    public boolean isValidPosition( IBlockState state, IWorldReaderBase world, BlockPos pos )
    {
        EnumFacing facing = state.get( FACING );
        BlockPos offsetPos = pos.offset( facing );
        IBlockState offsetState = world.getBlockState( offsetPos );
        return offsetState.getBlockFaceShape( world, offsetPos, facing.getOpposite() ) == BlockFaceShape.SOLID;
    }

    @Nullable
    @Override
    public IBlockState getStateForPlacement( BlockItemUseContext placement )
    {
        return getDefaultState()
            .with( FACING, placement.getFace().getOpposite() )
            .with( WATERLOGGED, getWaterloggedStateForPlacement( placement ) );
    }

    @Override
    @Deprecated
    public final boolean isFullCube( IBlockState state )
    {
        return false;
    }

    @Nonnull
    @Override
    @Deprecated
    public BlockFaceShape getBlockFaceShape( IBlockReader worldIn, IBlockState state, BlockPos pos, EnumFacing face )
    {
        return BlockFaceShape.UNDEFINED;
    }
}
