/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2017. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.peripheral.modem;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.api.network.IPacketNetwork;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public abstract class WirelessModemPeripheral extends ModemPeripheral
{
    private boolean m_advanced;

    public WirelessModemPeripheral( boolean advanced )
    {
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
                Vec3d position = getPosition();
                double minRange = ComputerCraft.modem_range;
                double maxRange = ComputerCraft.modem_highAltitudeRange;
                if( world.isRaining() && world.isThundering() )
                {
                    minRange = ComputerCraft.modem_rangeDuringStorm;
                    maxRange = ComputerCraft.modem_highAltitudeRangeDuringStorm;
                }
                if( position.y > 96.0 && maxRange > minRange )
                {
                    return minRange + ( position.y - 96.0 ) * ( ( maxRange - minRange ) / ( ( world.getHeight() - 1 ) - 96.0 ) );
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
