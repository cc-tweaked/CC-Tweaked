/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.pocket.peripherals;

import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.shared.peripheral.modem.ModemState;
import dan200.computercraft.shared.peripheral.modem.wireless.WirelessModemPeripheral;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nonnull;

public class PocketModemPeripheral extends WirelessModemPeripheral
{
    private Level world = null;
    private Vec3 position = Vec3.ZERO;

    public PocketModemPeripheral( boolean advanced )
    {
        super( new ModemState(), advanced );
    }

    void setLocation( Level world, Vec3 position )
    {
        this.position = position;
        this.world = world;
    }

    @Nonnull
    @Override
    public Level getLevel()
    {
        return world;
    }

    @Nonnull
    @Override
    public Vec3 getPosition()
    {
        return position;
    }

    @Override
    public boolean equals( IPeripheral other )
    {
        return other instanceof PocketModemPeripheral;
    }
}
