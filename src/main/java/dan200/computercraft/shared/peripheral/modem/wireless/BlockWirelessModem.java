/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.peripheral.modem.wireless;

import dan200.computercraft.shared.common.BlockGeneric;
import dan200.computercraft.shared.peripheral.modem.ModemShapes;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.IWaterLoggable;
import net.minecraft.fluid.FluidState;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.state.BooleanProperty;
import net.minecraft.state.DirectionProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.IWorld;
import net.minecraft.world.IWorldReader;
import net.minecraftforge.fml.RegistryObject;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static dan200.computercraft.shared.util.WaterloggableHelpers.*;

public class BlockWirelessModem extends BlockGeneric implements IWaterLoggable
{
    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    public static final BooleanProperty ON = BooleanProperty.create( "on" );

    public BlockWirelessModem( Properties settings, RegistryObject<? extends TileEntityType<? extends TileWirelessModem>> type )
    {
        super( settings, type );
        setDefaultState( getStateContainer().getBaseState()
            .with( FACING, Direction.NORTH )
            .with( ON, false )
            .with( WATERLOGGED, false ) );
    }

    @Override
    protected void fillStateContainer( StateContainer.Builder<Block, BlockState> builder )
    {
        builder.add( FACING, ON, WATERLOGGED );
    }

    @Nonnull
    @Override
    @Deprecated
    public VoxelShape getShape( BlockState blockState, @Nonnull IBlockReader blockView, @Nonnull BlockPos blockPos, @Nonnull ISelectionContext context )
    {
        return ModemShapes.getBounds( blockState.get( FACING ) );
    }

    @Nonnull
    @Override
    @Deprecated
    public FluidState getFluidState( @Nonnull BlockState state )
    {
        return getWaterloggedFluidState( state );
    }

    @Nonnull
    @Override
    @Deprecated
    public BlockState updatePostPlacement( @Nonnull BlockState state, @Nonnull Direction side, @Nonnull BlockState otherState, @Nonnull IWorld world, @Nonnull BlockPos pos, @Nonnull BlockPos otherPos )
    {
        updateWaterloggedPostPlacement( state, world, pos );
        return side == state.get( FACING ) && !state.isValidPosition( world, pos )
            ? state.getFluidState().getBlockState()
            : state;
    }

    @Override
    @Deprecated
    public boolean isValidPosition( BlockState state, IWorldReader world, BlockPos pos )
    {
        Direction facing = state.get( FACING );
        BlockPos offsetPos = pos.offset( facing );
        BlockState offsetState = world.getBlockState( offsetPos );
        return hasSolidSide( offsetState, world, offsetPos, facing.getOpposite() );
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement( BlockItemUseContext placement )
    {
        return getDefaultState()
            .with( FACING, placement.getFace().getOpposite() )
            .with( WATERLOGGED, getWaterloggedStateForPlacement( placement ) );
    }
}
