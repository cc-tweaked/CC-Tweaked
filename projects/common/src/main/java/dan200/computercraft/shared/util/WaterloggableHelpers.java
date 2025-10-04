// SPDX-FileCopyrightText: 2019 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;

/**
 * Helpers for working with waterlogged blocks.
 */
public final class WaterloggableHelpers {
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

    private WaterloggableHelpers() {
    }

    /**
     * Call from {@link net.minecraft.world.level.block.Block#getFluidState(BlockState)}.
     *
     * @param state The current state
     * @return This waterlogged block's current fluid
     */
    public static FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : Fluids.EMPTY.defaultFluidState();
    }

    /**
     * Call from {@link net.minecraft.world.level.block.Block#updateShape(BlockState, LevelReader, ScheduledTickAccess, BlockPos, Direction, BlockPos, BlockState, RandomSource)}.
     *
     * @param state  The current state
     * @param level  The position of this block
     * @param ticker The ticker to schedule with.
     * @param pos    The world this block exists in
     */
    public static void updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticker, BlockPos pos) {
        if (state.getValue(WATERLOGGED)) {
            ticker.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }
    }

    public static boolean getFluidStateForPlacement(BlockPlaceContext context) {
        return context.getLevel().getFluidState(context.getClickedPos()).getType() == Fluids.WATER;
    }
}
