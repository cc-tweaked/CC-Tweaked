/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2017. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.peripheral.common;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.shared.peripheral.PeripheralType;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import javax.annotation.Nonnull;

public class PeripheralItemFactory
{
    @Nonnull
    public static ItemStack create( IPeripheralTile tile )
    {
        return create( tile.getPeripheralType(), tile.getLabel(), 1 );
    }

    @Nonnull
    public static ItemStack create( PeripheralType type, String label, int quantity )
    {
        ItemPeripheral peripheral = ((ItemPeripheral) Item.getItemFromBlock( ComputerCraft.Blocks.peripheral ));
        ItemCable cable = ((ItemCable) Item.getItemFromBlock( ComputerCraft.Blocks.cable ));
        switch( type )
        {
            case DiskDrive:
            {
                ItemStack stack = new ItemStack( ComputerCraft.Blocks.diskDrive, quantity );
                if( label != null ) stack.setStackDisplayName( label );
                return stack;
            }
            case WirelessModem:
                return new ItemStack( ComputerCraft.Blocks.wirelessModem, quantity );
            case AdvancedModem:
                return new ItemStack( ComputerCraft.Blocks.advancedModem, quantity );

            case Speaker:
            case Printer:
            case Monitor:
            case AdvancedMonitor:
            {
                return peripheral.create( type, label, quantity );
            }
            case WiredModem:
            case Cable:
            {
                return cable.create( type, label, quantity );
            }
            case WiredModemFull:
                return new ItemStack( ComputerCraft.Blocks.wiredModemFull, quantity );
        }
        return ItemStack.EMPTY;
    }
}
