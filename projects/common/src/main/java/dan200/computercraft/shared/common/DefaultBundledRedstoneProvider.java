// Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
//
// SPDX-License-Identifier: LicenseRef-CCPL

package dan200.computercraft.shared.common;

import dan200.computercraft.api.redstone.BundledRedstoneProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;


public class DefaultBundledRedstoneProvider implements BundledRedstoneProvider {
    @Override
    public int getBundledRedstoneOutput(Level world, BlockPos pos, Direction side) {
        return getDefaultBundledRedstoneOutput(world, pos, side);
    }

    public static int getDefaultBundledRedstoneOutput(Level world, BlockPos pos, Direction side) {
        var block = world.getBlockState(pos).getBlock();
        return block instanceof IBundledRedstoneBlock bundledBlock ? bundledBlock.getBundledRedstoneOutput(world, pos, side) : -1;
    }
}
