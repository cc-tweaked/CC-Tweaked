/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.network.client;

import dan200.computercraft.client.sound.SpeakerManager;
import dan200.computercraft.shared.network.NetworkMessage;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.network.NetworkEvent;

import javax.annotation.Nonnull;
import java.util.UUID;

/**
 * Starts a sound on the client.
 *
 * Used by speakers to play sounds.
 *
 * @see dan200.computercraft.shared.peripheral.speaker.TileSpeaker
 */
public class SpeakerPlayClientMessage implements NetworkMessage
{
    private final UUID source;
    private final Vector3d pos;
    private final ResourceLocation sound;
    private final float volume;
    private final float pitch;

    public SpeakerPlayClientMessage( UUID source, Vector3d pos, ResourceLocation event, float volume, float pitch )
    {
        this.source = source;
        this.pos = pos;
        sound = event;
        this.volume = volume;
        this.pitch = pitch;
    }

    public SpeakerPlayClientMessage( PacketBuffer buf )
    {
        source = buf.readUUID();
        pos = new Vector3d( buf.readDouble(), buf.readDouble(), buf.readDouble() );
        sound = buf.readResourceLocation();
        volume = buf.readFloat();
        pitch = buf.readFloat();
    }

    @Override
    public void toBytes( @Nonnull PacketBuffer buf )
    {
        buf.writeUUID( source );
        buf.writeDouble( pos.x() );
        buf.writeDouble( pos.y() );
        buf.writeDouble( pos.z() );
        buf.writeResourceLocation( sound );
        buf.writeFloat( volume );
        buf.writeFloat( pitch );
    }

    @Override
    @OnlyIn( Dist.CLIENT )
    public void handle( NetworkEvent.Context context )
    {
        SpeakerManager.getSound( source ).playSound( pos, sound, volume, pitch );
    }
}
