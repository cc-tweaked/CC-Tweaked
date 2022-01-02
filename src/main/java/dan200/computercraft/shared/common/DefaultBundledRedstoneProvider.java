/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.common;

import dan200.computercraft.api.redstone.IBundledRedstoneProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import javax.annotation.Nonnull;

public class DefaultBundledRedstoneProvider implements IBundledRedstoneProvider
{
    @Override
    public int getBundledRedstoneOutput( @Nonnull Level world, @Nonnull BlockPos pos, @Nonnull Direction side )
    {
        return getDefaultBundledRedstoneOutput( world, pos, side );
    }

    public static int getDefaultBundledRedstoneOutput( Level world, BlockPos pos, Direction side )
    {
        Block block = world.getBlockState( pos ).getBlock();
        if( block instanceof IBundledRedstoneBlock generic )
        {
            if( generic.getBundledRedstoneConnectivity( world, pos, side ) )
            {
                return generic.getBundledRedstoneOutput( world, pos, side );
            }
        }
        return -1;
    }
}
