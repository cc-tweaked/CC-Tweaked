/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.util;

import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

public final class RedstoneUtil
{
    public static void propagateRedstoneOutput( World world, BlockPos pos, Direction side )
    {
        // Propagate ordinary output. See BlockRedstoneDiode.notifyNeighbors
        BlockState block = world.getBlockState( pos );
        BlockPos neighbourPos = pos.offset( side );
        world.updateNeighbor( neighbourPos, block.getBlock(), pos );
        world.updateNeighborsExcept( neighbourPos, block.getBlock(), side.getOpposite() );
    }
}
