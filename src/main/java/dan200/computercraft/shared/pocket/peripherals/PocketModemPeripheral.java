/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.pocket.peripherals;

import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.shared.peripheral.modem.ModemState;
import dan200.computercraft.shared.peripheral.modem.wireless.WirelessModemPeripheral;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import javax.annotation.Nonnull;

public class PocketModemPeripheral extends WirelessModemPeripheral
{
    private World world = null;
    private Vec3d position = Vec3d.ZERO;

    public PocketModemPeripheral( boolean advanced )
    {
        super( new ModemState(), advanced );
    }

    void setLocation( World world, Vec3d position )
    {
        this.position = position;
        this.world = world;
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
        return position;
    }

    @Override
    public boolean equals( IPeripheral other )
    {
        return other instanceof PocketModemPeripheral;
    }
}
