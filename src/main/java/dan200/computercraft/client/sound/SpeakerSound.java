/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.client.sound;

import dan200.computercraft.shared.peripheral.speaker.SpeakerPosition;
import net.minecraft.client.audio.IAudioStream;
import net.minecraft.client.audio.ITickableSound;
import net.minecraft.client.audio.LocatableSound;
import net.minecraft.client.audio.SoundSource;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;

import javax.annotation.Nullable;
import java.util.concurrent.Executor;

public class SpeakerSound extends LocatableSound implements ITickableSound
{
    SoundSource source;
    Executor executor;
    DfpwmStream stream;

    private Entity entity;

    private boolean stopped = false;

    SpeakerSound( ResourceLocation sound, DfpwmStream stream, SpeakerPosition position, float volume, float pitch )
    {
        super( sound, SoundCategory.RECORDS );
        setPosition( position );
        this.stream = stream;
        this.volume = volume;
        this.pitch = pitch;
        attenuation = AttenuationType.LINEAR;
    }

    void setPosition( SpeakerPosition position )
    {
        x = position.position().x;
        y = position.position().y;
        z = position.position().z;
        entity = position.entity();
    }

    @Override
    public boolean isStopped()
    {
        return stopped;
    }

    @Override
    public void tick()
    {
        if( entity == null ) return;
        if( !entity.isAlive() )
        {
            stopped = true;
            looping = false;
        }
        else
        {
            x = entity.getX();
            y = entity.getY();
            z = entity.getZ();
        }
    }

    @Nullable
    public IAudioStream getStream()
    {
        return stream;
    }
}
