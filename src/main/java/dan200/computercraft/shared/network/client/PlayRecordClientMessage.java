/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2018. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.network.client;

import dan200.computercraft.shared.network.NetworkMessage;
import dan200.computercraft.shared.network.NetworkMessages;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;

import javax.annotation.Nonnull;

/**
 * Starts or stops a record on the client, depending on if {@link #getSoundEvent()} is {@code null}.
 *
 * Used by disk drives to play record items.
 *
 * @see dan200.computercraft.shared.peripheral.diskdrive.TileDiskDrive
 */
public class PlayRecordClientMessage implements NetworkMessage
{
    private BlockPos pos;
    private String name;
    private SoundEvent soundEvent;

    public PlayRecordClientMessage( BlockPos pos, SoundEvent event, String name )
    {
        this.pos = pos;
        this.name = name;
        this.soundEvent = event;
    }

    public PlayRecordClientMessage( BlockPos pos )
    {
        this.pos = pos;
    }

    public PlayRecordClientMessage()
    {
    }

    @Override
    public int getId()
    {
        return NetworkMessages.PLAY_RECORD_CLIENT_MESSAGE;
    }

    public BlockPos getPos()
    {
        return pos;
    }

    public String getName()
    {
        return name;
    }

    public SoundEvent getSoundEvent()
    {
        return soundEvent;
    }

    @Override
    public void toBytes( @Nonnull PacketBuffer buf )
    {
        buf.writeBlockPos( pos );
        if( soundEvent == null )
        {
            buf.writeBoolean( false );
        }
        else
        {
            buf.writeBoolean( true );
            buf.writeString( name );
            buf.writeInt( SoundEvent.REGISTRY.getIDForObject( soundEvent ) );
        }
    }

    @Override
    public void fromBytes( @Nonnull PacketBuffer buf )
    {
        pos = buf.readBlockPos();
        if( buf.readBoolean() )
        {
            name = buf.readString( Short.MAX_VALUE );
            soundEvent = SoundEvent.REGISTRY.getObjectById( buf.readInt() );
        }
    }
}
