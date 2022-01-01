/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.common;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

public interface IBundledRedstoneBlock
{
    boolean getBundledRedstoneConnectivity( Level world, BlockPos pos, Direction side );

    int getBundledRedstoneOutput( Level world, BlockPos pos, Direction side );
}
