/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.peripheral.common;

import dan200.computercraft.shared.peripheral.PeripheralType;
import net.minecraft.util.IStringSerializable;

import javax.annotation.Nonnull;

public enum BlockPeripheralVariant implements IStringSerializable
{
    DiskDriveEmpty( "disk_drive_empty", PeripheralType.DiskDrive ),
    DiskDriveFull( "disk_drive_full", PeripheralType.DiskDrive ),
    DiskDriveInvalid( "disk_drive_invalid", PeripheralType.DiskDrive ),
    PrinterEmpty( "printer_empty", PeripheralType.Printer ),
    PrinterTopFull( "printer_top_full", PeripheralType.Printer ),
    PrinterBottomFull( "printer_bottom_full", PeripheralType.Printer ),
    PrinterBothFull( "printer_both_full", PeripheralType.Printer ),
    WirelessModemOff( "wireless_modem_off", PeripheralType.WirelessModem ),
    WirelessModemOn( "wireless_modem_on", PeripheralType.WirelessModem ),
    WirelessModemUpOff( "wireless_modem_up_off", PeripheralType.WirelessModem ),
    WirelessModemUpOn( "wireless_modem_up_on", PeripheralType.WirelessModem ),
    WirelessModemDownOff( "wireless_modem_down_off", PeripheralType.WirelessModem ),
    WirelessModemDownOn( "wireless_modem_down_on", PeripheralType.WirelessModem ),
    Monitor( "monitor", PeripheralType.Monitor ),
    MonitorR( "monitor_r", PeripheralType.Monitor ),
    MonitorLR( "monitor_lr", PeripheralType.Monitor ),
    MonitorL( "monitor_l", PeripheralType.Monitor ),
    MonitorD( "monitor_d", PeripheralType.Monitor ),
    MonitorUD( "monitor_ud", PeripheralType.Monitor ),
    MonitorU( "monitor_u", PeripheralType.Monitor ),
    MonitorRD( "monitor_rd", PeripheralType.Monitor ),
    MonitorLRD( "monitor_lrd", PeripheralType.Monitor ),
    MonitorLD( "monitor_ld", PeripheralType.Monitor ),
    MonitorRUD( "monitor_rud", PeripheralType.Monitor ),
    MonitorLRUD( "monitor_lrud", PeripheralType.Monitor ),
    MonitorLUD( "monitor_lud", PeripheralType.Monitor ),
    MonitorRU( "monitor_ru", PeripheralType.Monitor ),
    MonitorLRU( "monitor_lru", PeripheralType.Monitor ),
    MonitorLU( "monitor_lu", PeripheralType.Monitor ),
    MonitorUp( "monitor_up", PeripheralType.Monitor ),
    MonitorUpR( "monitor_up_r", PeripheralType.Monitor ),
    MonitorUpLR( "monitor_up_lr", PeripheralType.Monitor ),
    MonitorUpL( "monitor_up_l", PeripheralType.Monitor ),
    MonitorUpD( "monitor_up_d", PeripheralType.Monitor ),
    MonitorUpUD( "monitor_up_ud", PeripheralType.Monitor ),
    MonitorUpU( "monitor_up_u", PeripheralType.Monitor ),
    MonitorUpRD( "monitor_up_rd", PeripheralType.Monitor ),
    MonitorUpLRD( "monitor_up_lrd", PeripheralType.Monitor ),
    MonitorUpLD( "monitor_up_ld", PeripheralType.Monitor ),
    MonitorUpRUD( "monitor_up_rud", PeripheralType.Monitor ),
    MonitorUpLRUD( "monitor_up_lrud", PeripheralType.Monitor ),
    MonitorUpLUD( "monitor_up_lud", PeripheralType.Monitor ),
    MonitorUpRU( "monitor_up_ru", PeripheralType.Monitor ),
    MonitorUpLRU( "monitor_up_lru", PeripheralType.Monitor ),
    MonitorUpLU( "monitor_up_lu", PeripheralType.Monitor ),
    MonitorDown( "monitor_down", PeripheralType.Monitor ),
    MonitorDownR( "monitor_down_r", PeripheralType.Monitor ),
    MonitorDownLR( "monitor_down_lr", PeripheralType.Monitor ),
    MonitorDownL( "monitor_down_l", PeripheralType.Monitor ),
    MonitorDownD( "monitor_down_d", PeripheralType.Monitor ),
    MonitorDownUD( "monitor_down_ud", PeripheralType.Monitor ),
    MonitorDownU( "monitor_down_u", PeripheralType.Monitor ),
    MonitorDownRD( "monitor_down_rd", PeripheralType.Monitor ),
    MonitorDownLRD( "monitor_down_lrd", PeripheralType.Monitor ),
    MonitorDownLD( "monitor_down_ld", PeripheralType.Monitor ),
    MonitorDownRUD( "monitor_down_rud", PeripheralType.Monitor ),
    MonitorDownLRUD( "monitor_down_lrud", PeripheralType.Monitor ),
    MonitorDownLUD( "monitor_down_lud", PeripheralType.Monitor ),
    MonitorDownRU( "monitor_down_ru", PeripheralType.Monitor ),
    MonitorDownLRU( "monitor_down_lru", PeripheralType.Monitor ),
    MonitorDownLU( "monitor_down_lu", PeripheralType.Monitor ),
    AdvancedMonitor( "advanced_monitor", PeripheralType.AdvancedMonitor ),
    AdvancedMonitorR( "advanced_monitor_r", PeripheralType.AdvancedMonitor ),
    AdvancedMonitorLR( "advanced_monitor_lr", PeripheralType.AdvancedMonitor ),
    AdvancedMonitorL( "advanced_monitor_l", PeripheralType.AdvancedMonitor ),
    AdvancedMonitorD( "advanced_monitor_d", PeripheralType.AdvancedMonitor ),
    AdvancedMonitorUD( "advanced_monitor_ud", PeripheralType.AdvancedMonitor ),
    AdvancedMonitorU( "advanced_monitor_u", PeripheralType.AdvancedMonitor ),
    AdvancedMonitorRD( "advanced_monitor_rd", PeripheralType.AdvancedMonitor ),
    AdvancedMonitorLRD( "advanced_monitor_lrd", PeripheralType.AdvancedMonitor ),
    AdvancedMonitorLD( "advanced_monitor_ld", PeripheralType.AdvancedMonitor ),
    AdvancedMonitorRUD( "advanced_monitor_rud", PeripheralType.AdvancedMonitor ),
    AdvancedMonitorLRUD( "advanced_monitor_lrud", PeripheralType.AdvancedMonitor ),
    AdvancedMonitorLUD( "advanced_monitor_lud", PeripheralType.AdvancedMonitor ),
    AdvancedMonitorRU( "advanced_monitor_ru", PeripheralType.AdvancedMonitor ),
    AdvancedMonitorLRU( "advanced_monitor_lru", PeripheralType.AdvancedMonitor ),
    AdvancedMonitorLU( "advanced_monitor_lu", PeripheralType.AdvancedMonitor ),
    AdvancedMonitorUp( "advanced_monitor_up", PeripheralType.AdvancedMonitor ),
    AdvancedMonitorUpR( "advanced_monitor_up_r", PeripheralType.AdvancedMonitor ),
    AdvancedMonitorUpLR( "advanced_monitor_up_lr", PeripheralType.AdvancedMonitor ),
    AdvancedMonitorUpL( "advanced_monitor_up_l", PeripheralType.AdvancedMonitor ),
    AdvancedMonitorUpD( "advanced_monitor_up_d", PeripheralType.AdvancedMonitor ),
    AdvancedMonitorUpUD( "advanced_monitor_up_ud", PeripheralType.AdvancedMonitor ),
    AdvancedMonitorUpU( "advanced_monitor_up_u", PeripheralType.AdvancedMonitor ),
    AdvancedMonitorUpRD( "advanced_monitor_up_rd", PeripheralType.AdvancedMonitor ),
    AdvancedMonitorUpLRD( "advanced_monitor_up_lrd", PeripheralType.AdvancedMonitor ),
    AdvancedMonitorUpLD( "advanced_monitor_up_ld", PeripheralType.AdvancedMonitor ),
    AdvancedMonitorUpRUD( "advanced_monitor_up_rud", PeripheralType.AdvancedMonitor ),
    AdvancedMonitorUpLRUD( "advanced_monitor_up_lrud", PeripheralType.AdvancedMonitor ),
    AdvancedMonitorUpLUD( "advanced_monitor_up_lud", PeripheralType.AdvancedMonitor ),
    AdvancedMonitorUpRU( "advanced_monitor_up_ru", PeripheralType.AdvancedMonitor ),
    AdvancedMonitorUpLRU( "advanced_monitor_up_lru", PeripheralType.AdvancedMonitor ),
    AdvancedMonitorUpLU( "advanced_monitor_up_lu", PeripheralType.AdvancedMonitor ),
    AdvancedMonitorDown( "advanced_monitor_down", PeripheralType.AdvancedMonitor ),
    AdvancedMonitorDownR( "advanced_monitor_down_r", PeripheralType.AdvancedMonitor ),
    AdvancedMonitorDownLR( "advanced_monitor_down_lr", PeripheralType.AdvancedMonitor ),
    AdvancedMonitorDownL( "advanced_monitor_down_l", PeripheralType.AdvancedMonitor ),
    AdvancedMonitorDownD( "advanced_monitor_down_d", PeripheralType.AdvancedMonitor ),
    AdvancedMonitorDownUD( "advanced_monitor_down_ud", PeripheralType.AdvancedMonitor ),
    AdvancedMonitorDownU( "advanced_monitor_down_u", PeripheralType.AdvancedMonitor ),
    AdvancedMonitorDownRD( "advanced_monitor_down_rd", PeripheralType.AdvancedMonitor ),
    AdvancedMonitorDownLRD( "advanced_monitor_down_lrd", PeripheralType.AdvancedMonitor ),
    AdvancedMonitorDownLD( "advanced_monitor_down_ld", PeripheralType.AdvancedMonitor ),
    AdvancedMonitorDownRUD( "advanced_monitor_down_rud", PeripheralType.AdvancedMonitor ),
    AdvancedMonitorDownLRUD( "advanced_monitor_down_lrud", PeripheralType.AdvancedMonitor ),
    AdvancedMonitorDownLUD( "advanced_monitor_down_lud", PeripheralType.AdvancedMonitor ),
    AdvancedMonitorDownRU( "advanced_monitor_down_ru", PeripheralType.AdvancedMonitor ),
    AdvancedMonitorDownLRU( "advanced_monitor_down_lru", PeripheralType.AdvancedMonitor ),
    AdvancedMonitorDownLU( "advanced_monitor_down_lu", PeripheralType.AdvancedMonitor ),
    Speaker( "speaker", PeripheralType.Speaker );

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
