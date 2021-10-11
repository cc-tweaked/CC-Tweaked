/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.network.client;

import dan200.computercraft.client.SoundManager;
import dan200.computercraft.shared.network.NetworkMessage;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.network.PacketContext;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;

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
    private final Vec3d pos;
    private final Identifier sound;
    private final float volume;
    private final float pitch;

    public SpeakerPlayClientMessage( UUID source, Vec3d pos, Identifier event, float volume, float pitch )
    {
        this.source = source;
        this.pos = pos;
        sound = event;
        this.volume = volume;
        this.pitch = pitch;
    }

    public SpeakerPlayClientMessage( PacketByteBuf buf )
    {
        source = buf.readUuid();
        pos = new Vec3d( buf.readDouble(), buf.readDouble(), buf.readDouble() );
        sound = buf.readIdentifier();
        volume = buf.readFloat();
        pitch = buf.readFloat();
    }

    @Override
    public void toBytes( @Nonnull PacketByteBuf buf )
    {
        buf.writeUuid( source );
        buf.writeDouble( pos.getX() );
        buf.writeDouble( pos.getY() );
        buf.writeDouble( pos.getZ() );
        buf.writeIdentifier( sound );
        buf.writeFloat( volume );
        buf.writeFloat( pitch );
    }

    @Override
    @Environment( EnvType.CLIENT )
    public void handle( PacketContext context )
    {
        SoundManager.playSound( source, pos, sound, volume, pitch );
    }
}
