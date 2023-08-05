// Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
//
// SPDX-License-Identifier: LicenseRef-CCPL

package dan200.computercraft.shared.util;

import dan200.computercraft.shared.platform.PlatformHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DiodeBlock;
import net.minecraft.world.level.block.RedStoneWireBlock;
import net.minecraft.world.level.block.state.BlockState;

public final class RedstoneUtil {
    private RedstoneUtil() {
    }

    /**
     * Gets the redstone input for an adjacent block.
     *
     * @param world The world we exist in
     * @param pos   The position of the neighbour
     * @param side  The side we are reading from
     * @return The effective redstone power
     * @see DiodeBlock#getInputSignal(Level, BlockPos, BlockState)
     */
    public static int getRedstoneInput(Level world, BlockPos pos, Direction side) {
        var power = world.getSignal(pos, side);
        if (power >= 15) return power;

        var neighbour = world.getBlockState(pos);
        return neighbour.getBlock() == Blocks.REDSTONE_WIRE
            ? Math.max(power, neighbour.getValue(RedStoneWireBlock.POWER))
            : power;
    }

    /**
     * Propagate a redstone output to a particular side.
     *
     * @param world The current level.
     * @param pos   The current block's position.
     * @param side  The direction we're propagating to.
     * @see DiodeBlock#updateNeighborsInFront(Level, BlockPos, BlockState)
     */
    public static void propagateRedstoneOutput(Level world, BlockPos pos, Direction side) {
        var block = world.getBlockState(pos);
        if (!PlatformHelper.get().onNotifyNeighbour(world, pos, block, side)) return;

        var neighbourPos = pos.relative(side);
        world.neighborChanged(neighbourPos, block.getBlock(), pos);
        // We intentionally use updateNeighborsAt here instead of updateNeighborsAtExceptFromFacing, as computers can
        // both send and receive redstone, and so also need to be updated.
        world.updateNeighborsAt(neighbourPos, block.getBlock());
    }
}
