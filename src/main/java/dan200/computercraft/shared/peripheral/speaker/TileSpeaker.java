/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.peripheral.speaker;

import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.shared.peripheral.common.TilePeripheralBase;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class TileSpeaker extends TilePeripheralBase
{
    // Statics
    public static final int MIN_TICKS_BETWEEN_SOUNDS = 1;

    // Members
    private final SpeakerPeripheral m_peripheral;

    public TileSpeaker()
    {
        m_peripheral = new Peripheral( this );
    }

    @Override
    public void update()
    {
        m_peripheral.update();
    }

    // IPeripheralTile implementation

    @Override
    public IPeripheral getPeripheral( @Nonnull EnumFacing side )
    {
        return m_peripheral;
    }

    private static final class Peripheral extends SpeakerPeripheral
    {
        private final TileSpeaker speaker;

        private Peripheral( TileSpeaker speaker )
        {
            this.speaker = speaker;
        }

        @Override
        public World getWorld()
        {
            return speaker.getWorld();
        }

        @Override
        public Vec3d getPosition()
        {
            BlockPos pos = speaker.getPos();
            return new Vec3d( pos.getX(), pos.getY(), pos.getZ() );
        }

        @Override
        public boolean equals( @Nullable IPeripheral other )
        {
            return this == other || (other instanceof Peripheral && speaker == ((Peripheral) other).speaker);
        }
    }
}
