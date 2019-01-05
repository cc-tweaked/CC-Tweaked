/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.util;

import dan200.computercraft.shared.BundledRedstone;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class RedstoneUtil
{
    @Deprecated
    public static int getRedstoneOutput( World world, BlockPos pos, EnumFacing side )
    {
        return world.getRedstonePower( pos, side );
    }

    @Deprecated
    public static int getBundledRedstoneOutput( World world, BlockPos pos, EnumFacing side )
    {
        return BundledRedstone.getOutput( world, pos, side );
    }

    public static void propagateRedstoneOutput( World world, BlockPos pos, EnumFacing side )
    {
        // Propagate ordinary output
        IBlockState block = world.getBlockState( pos );
        BlockPos neighbourPos = pos.offset( side );
        IBlockState neighbour = world.getBlockState( neighbourPos );
        if( neighbour.getBlock() != Blocks.AIR )
        {
            world.neighborChanged( neighbourPos, block.getBlock(), pos );
            if( neighbour.getBlock().isNormalCube( neighbour, world, neighbourPos ) )
            {
                world.notifyNeighborsOfStateExcept( neighbourPos, block.getBlock(), side.getOpposite() );
            }
        }
    }
}
