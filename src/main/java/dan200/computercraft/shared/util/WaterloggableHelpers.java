/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;

/**
 * Helpers for working with waterlogged blocks.
 */
public final class WaterloggableHelpers
{
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

    private WaterloggableHelpers()
    {
    }

    /**
     * Call from {@link net.minecraft.world.level.block.Block#getFluidState(BlockState)}.
     *
     * @param state The current state
     * @return This waterlogged block's current fluid
     */
    public static FluidState getFluidState( BlockState state )
    {
        return state.getValue( WATERLOGGED ) ? Fluids.WATER.getSource( false ) : Fluids.EMPTY.defaultFluidState();
    }

    /**
     * Call from {@link net.minecraft.world.level.block.Block#updateShape(BlockState, Direction, BlockState, LevelAccessor, BlockPos, BlockPos)}.
     *
     * @param state The current state
     * @param world The position of this block
     * @param pos   The world this block exists in
     */
    public static void updateShape( BlockState state, LevelAccessor world, BlockPos pos )
    {
        if( state.getValue( WATERLOGGED ) )
        {
            world.scheduleTick( pos, Fluids.WATER, Fluids.WATER.getTickDelay( world ) );
        }
    }

    public static boolean getFluidStateForPlacement( BlockPlaceContext context )
    {
        return context.getLevel().getFluidState( context.getClickedPos() ).getType() == Fluids.WATER;
    }
}
