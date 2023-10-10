// Copyright Daniel Ratcliffe, 2011-2022. This API may be redistributed unmodified and in full only.
//
// SPDX-License-Identifier: LicenseRef-CCPL

package dan200.computercraft.api.redstone;

import dan200.computercraft.api.ComputerCraftAPI;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

/**
 * This interface is used to provide bundled redstone output for blocks.
 *
 * @see ComputerCraftAPI#registerBundledRedstoneProvider(BundledRedstoneProvider)
 */
@FunctionalInterface
public interface BundledRedstoneProvider {
    /**
     * Produce an bundled redstone output from a block location.
     *
     * @param world The world this block is in.
     * @param pos   The position this block is at.
     * @param side  The side to extract the bundled redstone output from.
     * @return A number in the range 0-65535 to indicate this block is providing output, or -1 if you do not wish to
     * handle this block.
     * @see ComputerCraftAPI#registerBundledRedstoneProvider(BundledRedstoneProvider)
     */
    int getBundledRedstoneOutput(Level world, BlockPos pos, Direction side);
}
