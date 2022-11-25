/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
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
        if (block instanceof IBundledRedstoneBlock generic) {
            if (generic.getBundledRedstoneConnectivity(world, pos, side)) {
                return generic.getBundledRedstoneOutput(world, pos, side);
            }
        }
        return -1;
    }
}
