/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
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

    void setLocation( World world, double x, double y, double z )
    {
        position = new Vec3d( x, y, z );
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
