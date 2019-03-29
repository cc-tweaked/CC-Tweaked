/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.peripheral;

import net.minecraft.util.IStringSerializable;

import javax.annotation.Nonnull;

public enum PeripheralType implements IStringSerializable
{
    DiskDrive( "disk_drive" ),
    Printer( "printer" ),
    Monitor( "monitor" ),
    AdvancedMonitor( "advanced_monitor" ),
    WirelessModem( "wireless_modem" ),
    WiredModem( "wired_modem" ),
    Cable( "cable" ),
    WiredModemWithCable( "wired_modem_with_cable" ),
    AdvancedModem( "advanced_modem" ),
    Speaker( "speaker" ),
    WiredModemFull( "wired_modem_full" );

    private final String name;

    PeripheralType( String name )
    {
        this.name = name;
    }

    @Nonnull
    @Override
    public String getName()
    {
        return name;
    }

    @Override
    public String toString() { return name; }
}
