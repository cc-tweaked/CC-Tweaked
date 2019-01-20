/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.peripheral.common;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.shared.peripheral.PeripheralType;
import net.minecraft.item.ItemStack;

import javax.annotation.Nonnull;

public class PeripheralItemFactory
{
    @Nonnull
    public static ItemStack create( TilePeripheralBase tile )
    {
        return create( tile.getPeripheralType(), tile.getLabel(), 1 );
    }

    @Nonnull
    public static ItemStack create( PeripheralType type, String label, int quantity )
    {
        switch( type )
        {
            case Speaker:
            case DiskDrive:
            case Printer:
            case Monitor:
            case AdvancedMonitor:
            case WirelessModem:
                return ComputerCraft.Items.peripheral.create( type, label, quantity );
            case WiredModem:
            case Cable:
                return ComputerCraft.Items.cable.create( type, quantity );
            case AdvancedModem:
                return new ItemStack( ComputerCraft.Blocks.advancedModem, quantity );
            case WiredModemFull:
                return new ItemStack( ComputerCraft.Blocks.wiredModemFull, quantity );
        }
        return ItemStack.EMPTY;
    }
}
