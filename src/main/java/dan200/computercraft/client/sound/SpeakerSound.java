/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.client.sound;

import com.mojang.blaze3d.audio.Channel;
import net.minecraft.client.resources.sounds.AbstractSoundInstance;
import net.minecraft.client.resources.sounds.TickableSoundInstance;
import net.minecraft.client.sounds.AudioStream;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.concurrent.Executor;

public class SpeakerSound extends AbstractSoundInstance implements TickableSoundInstance
{
    Channel channel;
    Executor executor;
    DfpwmStream stream;

    SpeakerSound( ResourceLocation sound, DfpwmStream stream, Vec3 position, float volume, float pitch )
    {
        super( sound, SoundSource.RECORDS );
        setPosition( position );
        this.stream = stream;
        this.volume = volume;
        this.pitch = pitch;
        attenuation = Attenuation.LINEAR;
    }

    void setPosition( Vec3 position )
    {
        x = (float) position.x();
        y = (float) position.y();
        z = (float) position.z();
    }

    @Override
    public boolean isStopped()
    {
        return false;
    }

    @Override
    public void tick()
    {
    }

    @Nullable
    public AudioStream getStream()
    {
        return stream;
    }
}
