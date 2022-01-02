/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.peripheral.modem.wireless;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.api.network.IPacketNetwork;
import dan200.computercraft.shared.peripheral.modem.ModemPeripheral;
import dan200.computercraft.shared.peripheral.modem.ModemState;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public abstract class WirelessModemPeripheral extends ModemPeripheral
{
    public static final String NORMAL_ADJECTIVE = "upgrade.computercraft.wireless_modem_normal.adjective";
    public static final String ADVANCED_ADJECTIVE = "upgrade.computercraft.wireless_modem_advanced.adjective";

    private final boolean advanced;

    public WirelessModemPeripheral( ModemState state, boolean advanced )
    {
        super( state );
        this.advanced = advanced;
    }

    @Override
    public boolean isInterdimensional()
    {
        return advanced;
    }

    @Override
    public double getRange()
    {
        if( advanced )
        {
            return Integer.MAX_VALUE;
        }
        else
        {
            Level world = getLevel();
            if( world != null )
            {
                Vec3 position = getPosition();
                double minRange = ComputerCraft.modemRange;
                double maxRange = ComputerCraft.modemHighAltitudeRange;
                if( world.isRaining() && world.isThundering() )
                {
                    minRange = ComputerCraft.modemRangeDuringStorm;
                    maxRange = ComputerCraft.modemHighAltitudeRangeDuringStorm;
                }
                if( position.y > 96.0 && maxRange > minRange )
                {
                    return minRange + (position.y - 96.0) * ((maxRange - minRange) / ((world.getMaxBuildHeight() - 1) - 96.0));
                }
                return minRange;
            }
            return 0.0;
        }
    }

    @Override
    protected IPacketNetwork getNetwork()
    {
        return WirelessNetwork.getUniversal();
    }
}
