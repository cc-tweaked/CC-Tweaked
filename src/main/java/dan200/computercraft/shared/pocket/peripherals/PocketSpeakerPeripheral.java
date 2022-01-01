/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.pocket.peripherals;

import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.shared.peripheral.speaker.UpgradeSpeakerPeripheral;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;

import javax.annotation.Nonnull;

public class PocketSpeakerPeripheral extends UpgradeSpeakerPeripheral
{
    private World world = null;
    private Vector3d position = Vector3d.ZERO;

    void setLocation( World world, Vector3d position )
    {
        this.position = position;
        this.world = world;
    }

    @Override
    public World getWorld()
    {
        return world;
    }

    @Nonnull
    @Override
    public Vector3d getPosition()
    {
        return world != null ? position : null;
    }

    @Override
    public boolean equals( IPeripheral other )
    {
        return other instanceof PocketSpeakerPeripheral;
    }
}
