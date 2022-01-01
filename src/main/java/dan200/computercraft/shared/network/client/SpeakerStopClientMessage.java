/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.network.client;

import dan200.computercraft.client.sound.SpeakerManager;
import dan200.computercraft.shared.network.NetworkMessage;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;

import javax.annotation.Nonnull;
import java.util.UUID;

/**
 * Stops a sound on the client
 *
 * Called when a speaker is broken.
 *
 * @see dan200.computercraft.shared.peripheral.speaker.TileSpeaker
 */
public class SpeakerStopClientMessage implements NetworkMessage
{
    private final UUID source;

    public SpeakerStopClientMessage( UUID source )
    {
        this.source = source;
    }

    public SpeakerStopClientMessage( FriendlyByteBuf buf )
    {
        source = buf.readUUID();
    }

    @Override
    public void toBytes( @Nonnull FriendlyByteBuf buf )
    {
        buf.writeUUID( source );
    }

    @Override
    @OnlyIn( Dist.CLIENT )
    public void handle( NetworkEvent.Context context )
    {
        SpeakerManager.stopSound( source );
    }
}
