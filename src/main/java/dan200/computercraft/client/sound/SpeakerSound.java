/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.client.sound;

import com.mojang.blaze3d.audio.Channel;
import net.minecraft.client.resources.sounds.AbstractSoundInstance;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.resources.sounds.TickableSoundInstance;
import net.minecraft.client.sounds.AudioStream;
import net.minecraft.client.sounds.SoundBufferLibrary;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nonnull;
import java.util.concurrent.CompletableFuture;
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

    @Nonnull
    @Override
    public CompletableFuture<AudioStream> getStream( @Nonnull SoundBufferLibrary soundBuffers, @Nonnull Sound sound, boolean looping )
    {
        return stream != null ? CompletableFuture.completedFuture( stream ) : super.getStream( soundBuffers, sound, looping );
    }
}
