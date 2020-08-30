/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.pocket.peripherals;

import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.shared.peripheral.speaker.SpeakerPeripheral;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class PocketSpeakerPeripheral extends SpeakerPeripheral
{
    private World world = null;
    private Vec3d position = Vec3d.ZERO;

    void setLocation( World world, Vec3d position )
    {
        this.position = position;
        this.world = world;
    }

    @Override
    public World getWorld()
    {
        return world;
    }

    @Override
    public Vec3d getPosition()
    {
        return world != null ? position : null;
    }

    @Override
    public boolean equals( IPeripheral other )
    {
        return other instanceof PocketSpeakerPeripheral;
    }
}
