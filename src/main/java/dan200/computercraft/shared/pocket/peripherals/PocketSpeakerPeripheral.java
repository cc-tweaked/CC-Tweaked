/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2016. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.pocket.peripherals;

import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.shared.peripheral.speaker.SpeakerPeripheral;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class PocketSpeakerPeripheral extends SpeakerPeripheral
{
    private World m_world;
    private BlockPos m_position;

    PocketSpeakerPeripheral()
    {
        super();
        m_world = null;
        m_position = new BlockPos( 0.0, 0.0, 0.0 );
    }

    void setLocation( World world, double x, double y, double z )
    {
        m_position = new BlockPos( x, y, z );

        if( m_world != world )
        {
            m_world = world;
        }
    }

    @Override
    public World getWorld()
    {
        return m_world;
    }

    @Override
    public BlockPos getPos()
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
        // Sufficient because of use case: checking peripherals on individual pocket computers -- there will not be +1
        return other instanceof PocketSpeakerPeripheral;
    }
}
