/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.network.client;

import dan200.computercraft.client.sound.SpeakerManager;
import dan200.computercraft.shared.network.NetworkMessage;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;

import javax.annotation.Nonnull;
import java.util.UUID;

/**
 * Starts a sound on the client.
 *
 * Used by speakers to play sounds.
 *
 * @see dan200.computercraft.shared.peripheral.speaker.TileSpeaker
 */
public class SpeakerMoveClientMessage implements NetworkMessage
{
    private final UUID source;
    private final Vec3 pos;

    public SpeakerMoveClientMessage( UUID source, Vec3 pos )
    {
        this.source = source;
        this.pos = pos;
    }

    public SpeakerMoveClientMessage( FriendlyByteBuf buf )
    {
        source = buf.readUUID();
        pos = new Vec3( buf.readDouble(), buf.readDouble(), buf.readDouble() );
    }

    @Override
    public void toBytes( @Nonnull FriendlyByteBuf buf )
    {
        buf.writeUUID( source );
        buf.writeDouble( pos.x() );
        buf.writeDouble( pos.y() );
        buf.writeDouble( pos.z() );
    }

    @Override
    @OnlyIn( Dist.CLIENT )
    public void handle( NetworkEvent.Context context )
    {
        SpeakerManager.moveSound( source, pos );
    }
}
