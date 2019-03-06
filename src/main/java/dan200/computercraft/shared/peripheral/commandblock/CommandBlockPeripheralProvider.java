/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.peripheral.commandblock;

import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.api.peripheral.IPeripheralProvider;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.CommandBlockBlockEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

import javax.annotation.Nonnull;

public class CommandBlockPeripheralProvider implements IPeripheralProvider
{
    @Override
    public IPeripheral getPeripheral( @Nonnull World world, @Nonnull BlockPos pos, @Nonnull Direction side )
    {
        BlockEntity tile = world.getBlockEntity( pos );
        if( tile instanceof CommandBlockBlockEntity )
        {
            CommandBlockBlockEntity commandBlock = (CommandBlockBlockEntity) tile;
            return new CommandBlockPeripheral( commandBlock );
        }
        return null;
    }
}
