/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.client.sound;

import net.minecraft.client.audio.IAudioStream;
import net.minecraft.client.audio.ITickableSound;
import net.minecraft.client.audio.LocatableSound;
import net.minecraft.client.audio.SoundSource;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.vector.Vector3d;

import javax.annotation.Nullable;
import java.util.concurrent.Executor;

public class SpeakerSound extends LocatableSound implements ITickableSound
{
    SoundSource source;
    Executor executor;
    DfpwmStream stream;

    SpeakerSound( ResourceLocation sound, DfpwmStream stream, Vector3d position, float volume, float pitch )
    {
        super( sound, SoundCategory.RECORDS );
        setPosition( position );
        this.stream = stream;
        this.volume = volume;
        this.pitch = pitch;
        attenuation = AttenuationType.LINEAR;
    }

    void setPosition( Vector3d position )
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
    public IAudioStream getStream()
    {
        return stream;
    }
}
