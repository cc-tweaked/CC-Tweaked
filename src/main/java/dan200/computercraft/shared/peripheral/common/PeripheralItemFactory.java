/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2017. Do not distribute without permission.
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
    public static ItemStack create( PeripheralType type, String label, int quantity )
    {
        switch( type )
        {
            case DiskDrive:
            {
                ItemStack stack = new ItemStack( ComputerCraft.Blocks.diskDrive, quantity );
                if( label != null ) stack.setStackDisplayName( label );
                return stack;
            }
            case Printer:
            {
                ItemStack stack = new ItemStack( ComputerCraft.Blocks.printer, quantity );
                if( label != null ) stack.setStackDisplayName( label );
                return stack;
            }
            case WirelessModem:
                return new ItemStack( ComputerCraft.Blocks.wirelessModem, quantity );
            case AdvancedModem:
                return new ItemStack( ComputerCraft.Blocks.advancedModem, quantity );
            case Speaker:
                return new ItemStack( ComputerCraft.Blocks.speaker, quantity );
            case Monitor:
                return new ItemStack( ComputerCraft.Blocks.monitorNormal, quantity );
            case AdvancedMonitor:
                return new ItemStack( ComputerCraft.Blocks.monitorAdvanced, quantity );
            case WiredModem:
                return new ItemStack( ComputerCraft.Items.wiredModem, quantity );
            case Cable:
                return new ItemStack( ComputerCraft.Items.cable, quantity );
            case WiredModemFull:
                return new ItemStack( ComputerCraft.Blocks.wiredModemFull, quantity );

            default:
                return ItemStack.EMPTY;
        }
    }
}
