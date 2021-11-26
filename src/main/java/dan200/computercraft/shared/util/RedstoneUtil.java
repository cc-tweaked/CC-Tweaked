/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public final class RedstoneUtil
{
    public static void propagateRedstoneOutput( Level world, BlockPos pos, Direction side )
    {
        // Propagate ordinary output. See BlockRedstoneDiode.notifyNeighbors
        BlockState block = world.getBlockState( pos );
        BlockPos neighbourPos = pos.relative( side );
        world.neighborChanged( neighbourPos, block.getBlock(), pos );
        world.updateNeighborsAtExceptFromFacing( neighbourPos, block.getBlock(), side.getOpposite() );
    }
}
