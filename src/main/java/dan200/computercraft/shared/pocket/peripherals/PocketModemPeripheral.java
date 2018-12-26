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
    private World world;
    private Vec3d position;

    public PocketModemPeripheral( boolean advanced )
    {
        super( new ModemState(), advanced );
        world = null;
        position = new Vec3d( 0.0, 0.0, 0.0 );
    }

    public void setLocation( World world, double x, double y, double z )
    {
        position = new Vec3d( x, y, z );
        if( this.world != world )
        {
            this.world = world;
            switchNetwork();
        }
    }

    @Nonnull
    @Override
    public World getWorld()
    {
        return world;
    }

    @Nonnull
    @Override
    public Vec3d getPosition()
    {
        return world != null ? position : null;
    }

    @Override
    public boolean equals( IPeripheral other )
    {
        return other instanceof PocketModemPeripheral;
    }
}
