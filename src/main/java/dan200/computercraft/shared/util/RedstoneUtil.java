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

public class RedstoneUtil
{
    public static void propagateRedstoneOutput( World world, BlockPos pos, Direction side )
    {
        // Propagate ordinary output
        BlockState block = world.getBlockState( pos );
        BlockPos neighbourPos = pos.offset( side );
        BlockState neighbour = world.getBlockState( neighbourPos );
        if( !neighbour.isAir() )
        {
            world.updateNeighbor( neighbourPos, block.getBlock(), pos );
            if( neighbour.isSimpleFullBlock( world, neighbourPos ) )
            {
                world.updateNeighborsExcept( neighbourPos, block.getBlock(), side.getOpposite() );
            }
        }
    }
}
