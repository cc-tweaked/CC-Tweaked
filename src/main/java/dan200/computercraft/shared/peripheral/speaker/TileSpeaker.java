/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.peripheral.speaker;

import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.api.peripheral.IPeripheralTile;
import dan200.computercraft.shared.common.TileGeneric;
import dan200.computercraft.shared.network.NetworkHandler;
import dan200.computercraft.shared.network.client.SpeakerStopClientMessage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.UUID;

public class TileSpeaker extends TileGeneric implements IPeripheralTile
{
    private final SpeakerPeripheral peripheral;
    private final UUID source = UUID.randomUUID();

    public TileSpeaker( BlockEntityType<TileSpeaker> type, BlockPos pos, BlockState state )
    {
        super( type, pos, state );
        peripheral = new Peripheral( this );
    }

    protected void serverTick()
    {
        peripheral.update();
    }

    @Override
    public void setRemoved()
    {
        super.setRemoved();
        if( level != null && !level.isClientSide )
        {
            NetworkHandler.sendToAllPlayers( new SpeakerStopClientMessage( peripheral.getSource() ) );
        }
    }

    @Nonnull
    @Override
    public IPeripheral getPeripheral( @Nonnull Direction side )
    {
        return peripheral;
    }

    private static final class Peripheral extends SpeakerPeripheral
    {
        private final TileSpeaker speaker;

        private Peripheral( TileSpeaker speaker )
        {
            this.speaker = speaker;
        }

        @Override
        public Level getLevel()
        {
            return speaker.getLevel();
        }

        @Nonnull
        @Override
        public Vec3 getPosition()
        {
            BlockPos pos = speaker.getBlockPos();
            return new Vec3( pos.getX(), pos.getY(), pos.getZ() );
        }

        @Override
        public boolean equals( @Nullable IPeripheral other )
        {
            return this == other || (other instanceof Peripheral && speaker == ((Peripheral) other).speaker);
        }
    }
}
