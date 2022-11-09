/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.client.sound;

import dan200.computercraft.shared.peripheral.speaker.SpeakerPosition;
import net.minecraft.client.resources.sounds.AbstractSoundInstance;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.resources.sounds.TickableSoundInstance;
import net.minecraft.client.sounds.AudioStream;
import net.minecraft.client.sounds.SoundBufferLibrary;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;

import javax.annotation.Nullable;
import java.util.concurrent.CompletableFuture;

public class SpeakerSound extends AbstractSoundInstance implements TickableSoundInstance {
    @Nullable
    DfpwmStream stream;

    private @Nullable Entity entity;

    private boolean stopped = false;

    SpeakerSound(ResourceLocation sound, @Nullable DfpwmStream stream, SpeakerPosition position, float volume, float pitch) {
        super(sound, SoundSource.RECORDS, SoundInstance.createUnseededRandom());
        setPosition(position);
        this.stream = stream;
        this.volume = volume;
        this.pitch = pitch;
        attenuation = Attenuation.LINEAR;
    }

    void setPosition(SpeakerPosition position) {
        x = position.position().x;
        y = position.position().y;
        z = position.position().z;
        entity = position.entity();
    }

    @Override
    public boolean isStopped() {
        return stopped;
    }

    @Override
    public void tick() {
        if (entity == null) return;
        if (!entity.isAlive()) {
            stopped = true;
            looping = false;
        } else {
            x = entity.getX();
            y = entity.getY();
            z = entity.getZ();
        }
    }

    @Override
    public CompletableFuture<AudioStream> getStream(SoundBufferLibrary soundBuffers, Sound sound, boolean looping) {
        return stream != null ? CompletableFuture.completedFuture(stream) : super.getStream(soundBuffers, sound, looping);
    }

    public @Nullable AudioStream getStream() {
        return stream;
    }
}
