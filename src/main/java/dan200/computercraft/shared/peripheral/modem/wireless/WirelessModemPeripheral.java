/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.peripheral.modem.wireless;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.api.network.IPacketNetwork;
import dan200.computercraft.shared.peripheral.modem.ModemPeripheral;
import dan200.computercraft.shared.peripheral.modem.ModemState;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;

public abstract class WirelessModemPeripheral extends ModemPeripheral
{
    private final boolean m_advanced;

    public WirelessModemPeripheral( ModemState state, boolean advanced )
    {
        super( state );
        m_advanced = advanced;
    }

    @Override
    public boolean isInterdimensional()
    {
        return m_advanced;
    }

    @Override
    public double getRange()
    {
        if( m_advanced )
        {
            return Integer.MAX_VALUE;
        }
        else
        {
            World world = getWorld();
            if( world != null )
            {
                Vector3d position = getPosition();
                double minRange = ComputerCraft.modemRange;
                double maxRange = ComputerCraft.modemHighAltitudeRange;
                if( world.isRaining() && world.isThundering() )
                {
                    minRange = ComputerCraft.modemRangeDuringStorm;
                    maxRange = ComputerCraft.modemHighAltitudeRangeDuringStorm;
                }
                if( position.y > 96.0 && maxRange > minRange )
                {
                    return minRange + (position.y - 96.0) * ((maxRange - minRange) / ((world.getHeight() - 1) - 96.0));
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
