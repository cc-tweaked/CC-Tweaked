/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.util;

import dan200.computercraft.shared.BundledRedstone;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.event.ForgeEventFactory;

import java.util.EnumSet;

public final class RedstoneUtil
{
    private RedstoneUtil() {}

    @Deprecated
    public static int getRedstoneOutput( World world, BlockPos pos, EnumFacing side )
    {
        return world.getRedstonePower( pos, side.getOpposite() );
    }

    @Deprecated
    public static int getBundledRedstoneOutput( World world, BlockPos pos, EnumFacing side )
    {
        return BundledRedstone.getOutput( world, pos, side );
    }

    public static void propagateRedstoneOutput( World world, BlockPos pos, EnumFacing side )
    {
        // Propagate ordinary output. See BlockRedstoneDiode.notifyNeighbors
        IBlockState block = world.getBlockState( pos );
        if( ForgeEventFactory.onNeighborNotify( world, pos, block, EnumSet.of( side ), false ).isCanceled() ) return;

        BlockPos neighbourPos = pos.offset( side );
        world.neighborChanged( neighbourPos, block.getBlock(), pos );
        world.notifyNeighborsOfStateExcept( neighbourPos, block.getBlock(), side.getOpposite() );
    }
}
