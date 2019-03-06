/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.peripheral.speaker;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.shared.common.TileGeneric;
import dan200.computercraft.shared.peripheral.IPeripheralTile;
import dan200.computercraft.shared.util.NamedTileEntityType;
import net.minecraft.util.Identifier;
import net.minecraft.util.Tickable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import javax.annotation.Nullable;

public class TileSpeaker extends TileGeneric implements Tickable, IPeripheralTile
{
    public static final int MIN_TICKS_BETWEEN_SOUNDS = 1;

    public static final NamedTileEntityType<TileSpeaker> FACTORY = NamedTileEntityType.create(
        new Identifier( ComputerCraft.MOD_ID, "speaker" ),
        TileSpeaker::new
    );

    private final SpeakerPeripheral m_peripheral;

    public TileSpeaker()
    {
        super( FACTORY );
        m_peripheral = new Peripheral( this );
    }

    @Override
    public void tick()
    {
        m_peripheral.update();
    }

    @Override
    public IPeripheral getPeripheral( Direction side )
    {
        return m_peripheral;
    }

    private static class Peripheral extends SpeakerPeripheral
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

