/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.common;

import dan200.computercraft.api.redstone.IBundledRedstoneProvider;
import net.minecraft.block.Block;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nonnull;

public class DefaultBundledRedstoneProvider implements IBundledRedstoneProvider
{
    @Override
    public int getBundledRedstoneOutput( @Nonnull World world, @Nonnull BlockPos pos, @Nonnull Direction side )
    {
        return getDefaultBundledRedstoneOutput( world, pos, side );
    }

    public static int getDefaultBundledRedstoneOutput( World world, BlockPos pos, Direction side )
    {
        Block block = world.getBlockState( pos ).getBlock();
        if( block instanceof IBundledRedstoneBlock )
        {
            IBundledRedstoneBlock generic = (IBundledRedstoneBlock) block;
            if( generic.getBundledRedstoneConnectivity( world, pos, side ) )
            {
                return generic.getBundledRedstoneOutput( world, pos, side );
            }
        }
        return -1;
    }
}
