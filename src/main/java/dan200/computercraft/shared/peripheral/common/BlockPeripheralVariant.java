/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2017. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.peripheral.common;

import dan200.computercraft.shared.peripheral.PeripheralType;
import net.minecraft.util.IStringSerializable;

import javax.annotation.Nonnull;

public enum BlockPeripheralVariant implements IStringSerializable
{
    PrinterEmpty( "printer_empty", PeripheralType.Printer ),
    PrinterTopFull( "printer_top_full", PeripheralType.Printer ),
    PrinterBottomFull( "printer_bottom_full", PeripheralType.Printer ),
    PrinterBothFull( "printer_both_full", PeripheralType.Printer );

    private String m_name;
    private PeripheralType m_peripheralType;

    BlockPeripheralVariant( String name, PeripheralType peripheralType )
    {
        m_name = name;
        m_peripheralType = peripheralType;
    }

    @Nonnull
    @Override
    public String getName()
    {
        return m_name;
    }

    public PeripheralType getPeripheralType()
    {
        return m_peripheralType;
    }

    @Override
    public String toString()
    {
        return getName();
    }
}
