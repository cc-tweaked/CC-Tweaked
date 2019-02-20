/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.util;

import net.minecraft.block.IBucketPickupHandler;
import net.minecraft.block.ILiquidContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.IFluidState;
import net.minecraft.init.Fluids;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.state.BooleanProperty;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.IWorld;

import javax.annotation.Nonnull;

/**
 * Represents a block which can be filled with water
 *
 * I'm fairly sure this exists on 1.14, but it's a useful convenience wrapper to have on 1.13.
 */
public interface WaterloggableBlock extends IBucketPickupHandler, ILiquidContainer
{
    BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

    /**
     * Call from {@link net.minecraft.block.Block#getFluidState(IBlockState)}
     *
     * @param state The current state
     * @return This waterlogged block's current fluid
     */
    default IFluidState getWaterloggedFluidState( IBlockState state )
    {
        return state.get( WATERLOGGED ) ? Fluids.WATER.getStillFluidState( false ) : Fluids.EMPTY.getDefaultState();
    }

    @Nonnull
    @Override
    default Fluid pickupFluid( @Nonnull IWorld world, @Nonnull BlockPos pos, @Nonnull IBlockState state )
    {
        if( state.get( WATERLOGGED ) )
        {
            world.setBlockState( pos, state.with( WATERLOGGED, false ), 3 );
            return Fluids.WATER;
        }
        else
        {
            return Fluids.EMPTY;
        }
    }

    @Override
    default boolean canContainFluid( @Nonnull IBlockReader world, @Nonnull BlockPos pos, @Nonnull IBlockState state, @Nonnull Fluid fluid )
    {
        return !state.get( WATERLOGGED ) && fluid == Fluids.WATER;
    }

    @Override
    default boolean receiveFluid( @Nonnull IWorld world, @Nonnull BlockPos pos, @Nonnull IBlockState state, @Nonnull IFluidState fluid )
    {
        if( !canContainFluid( world, pos, state, fluid.getFluid() ) ) return false;

        if( !world.isRemote() )
        {
            world.setBlockState( pos, state.with( WATERLOGGED, true ), 3 );
            world.getPendingFluidTicks().scheduleTick( pos, fluid.getFluid(), fluid.getFluid().getTickRate( world ) );
        }

        return true;
    }

    /**
     * Call from {@link net.minecraft.block.Block#updatePostPlacement(IBlockState, EnumFacing, IBlockState, IWorld, BlockPos, BlockPos)}
     *
     * @param state The current state
     * @param world The position of this block
     * @param pos   The world this block exists in
     */
    default void updateWaterloggedPostPlacement( IBlockState state, IWorld world, BlockPos pos )
    {
        if( state.get( WATERLOGGED ) )
        {
            world.getPendingFluidTicks().scheduleTick( pos, Fluids.WATER, Fluids.WATER.getTickRate( world ) );
        }
    }

    default boolean getWaterloggedStateForPlacement( BlockItemUseContext context )
    {
        return context.getWorld().getFluidState( context.getPos() ).getFluid() == Fluids.WATER;
    }
}
