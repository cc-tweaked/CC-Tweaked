/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2017. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.pocket.peripherals;

import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.shared.peripheral.modem.ModemState;
import dan200.computercraft.shared.peripheral.modem.WirelessModemPeripheral;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import javax.annotation.Nonnull;

public class PocketModemPeripheral extends WirelessModemPeripheral
{
    private World m_world;
    private Vec3d m_position;

    public PocketModemPeripheral( boolean advanced )
    {
        super( new ModemState(), advanced );
        m_world = null;
        m_position = new Vec3d( 0.0, 0.0, 0.0 );
    }

    public void setLocation( World world, double x, double y, double z )
    {
        m_position = new Vec3d( x, y, z );
        if( m_world != world )
        {
            m_world = world;
            switchNetwork();
        }
    }

    @Nonnull
    @Override
    public World getWorld()
    {
        return m_world;
    }

    @Nonnull
    @Override
    public Vec3d getPosition()
    {
        if( m_world != null )
        {
            return m_position;
        }
        return null;
    }

    @Override
    public boolean equals( IPeripheral other )
    {
        return other instanceof PocketModemPeripheral;
    }
}
