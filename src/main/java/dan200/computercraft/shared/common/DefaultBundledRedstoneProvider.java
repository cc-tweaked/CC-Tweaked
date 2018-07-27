/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2017. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.common;

import dan200.computercraft.api.redstone.IBundledRedstoneProvider;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nonnull;

public class DefaultBundledRedstoneProvider implements IBundledRedstoneProvider
{
    @Override
    public int getBundledRedstoneOutput( @Nonnull World world, @Nonnull BlockPos pos, @Nonnull EnumFacing side )
    {
        return getDefaultBundledRedstoneOutput( world, pos, side );
    }

    public static int getDefaultBundledRedstoneOutput( World world, BlockPos pos, EnumFacing side )
    {
        IBlockState state = world.getBlockState( pos );
        Block block = state.getBlock();
        if( block instanceof IBundledRedstoneBlock && block.canConnectRedstone( state, world, pos, side.getOpposite() ) )
        {
            IBundledRedstoneBlock generic = (IBundledRedstoneBlock) block;
            return generic.getBundledRedstoneOutput( state, world, pos, side );
        }

        return -1;
    }
}
