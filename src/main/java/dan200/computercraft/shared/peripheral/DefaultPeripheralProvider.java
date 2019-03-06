/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.peripheral;

import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.api.peripheral.IPeripheralProvider;
import dan200.computercraft.shared.computer.blocks.ComputerPeripheral;
import dan200.computercraft.shared.computer.blocks.TileComputerBase;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

import javax.annotation.Nonnull;

public class DefaultPeripheralProvider implements IPeripheralProvider
{
    @Override
    public IPeripheral getPeripheral( @Nonnull World world, @Nonnull BlockPos pos, @Nonnull Direction side )
    {
        BlockEntity tile = world.getBlockEntity( pos );
        if( tile == null ) return null;

        // Handle our peripherals
        if( tile instanceof IPeripheralTile )
        {
            IPeripheralTile peripheralTile = (IPeripheralTile) tile;
            return peripheralTile.getPeripheral( side );
        }

        // Handle our computers
        if( tile instanceof TileComputerBase )
        {
            TileComputerBase computerTile = (TileComputerBase) tile;
            /*
            if( tile instanceof TileTurtle )
            {
                if( !((TileTurtle) tile).hasMoved() )
                {
                    return new ComputerPeripheral( "turtle", computerTile.createProxy() );
                }
            }
            else
            */
            {
                return new ComputerPeripheral( "computer", computerTile.createProxy() );
            }
        }
        return null;
    }
}
