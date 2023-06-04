// SPDX-FileCopyrightText: 2021 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.client.sound;

import dan200.computercraft.annotations.FabricOverride;
import dan200.computercraft.annotations.ForgeOverride;
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

/**
 * A sound played by a speaker. This has two purposes:
 *
 * <ul>
 *     <li>Tracks a {@link SpeakerPosition}, ensuring the sound moves around with the speaker's owner.</li>
 *     <li>Provides a {@link DfpwmStream} when playing custom audio.</li>
 * </ul>
 */
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

    @ForgeOverride
    public CompletableFuture<AudioStream> getStream(SoundBufferLibrary soundBuffers, Sound sound, boolean looping) {
        return getAudioStream(soundBuffers, sound.getPath(), looping);
    }

    @FabricOverride
    public CompletableFuture<AudioStream> getAudioStream(SoundBufferLibrary soundBuffers, ResourceLocation sound, boolean looping) {
        return stream != null ? CompletableFuture.completedFuture(stream) : soundBuffers.getStream(sound, looping);
    }

    public @Nullable AudioStream getStream() {
        return stream;
    }
}
