// SPDX-FileCopyrightText: 2021 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.client.sound;

import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.core.util.Nullability;
import dan200.computercraft.shared.peripheral.speaker.EncodedAudio;
import dan200.computercraft.shared.peripheral.speaker.SpeakerPosition;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;

/**
 * An instance of a speaker, which is either playing a {@link DfpwmStream} stream or a normal sound.
 */
public class SpeakerInstance {
    public static final ResourceLocation DFPWM_STREAM = new ResourceLocation(ComputerCraftAPI.MOD_ID, "speaker.dfpwm_fake_audio_should_not_be_played");

    private @Nullable DfpwmStream currentStream;
    private @Nullable SpeakerSound sound;

    SpeakerInstance() {
    }

    private void pushAudio(EncodedAudio buffer) {
        var sound = this.sound;

        var stream = currentStream;
        if (stream == null) stream = currentStream = new DfpwmStream();
        var exhausted = stream.isEmpty();
        stream.push(buffer);

        // If we've got nothing left in the buffer, enqueue an additional one just in case.
        if (exhausted && sound != null && sound.stream == stream && stream.channel != null && stream.executor != null) {
            var actualStream = sound.stream;
            stream.executor.execute(() -> {
                var channel = Nullability.assertNonNull(actualStream.channel);
                if (!channel.stopped()) channel.pumpBuffers(1);
            });
        }
    }

    public void playAudio(SpeakerPosition position, float volume, EncodedAudio buffer) {
        pushAudio(buffer);

        var soundManager = Minecraft.getInstance().getSoundManager();

        if (sound != null && sound.stream != currentStream) {
            soundManager.stop(sound);
            sound = null;
        }

        if (sound != null && !soundManager.isActive(sound)) sound = null;

        if (sound == null && currentStream != null) {
            sound = new SpeakerSound(DFPWM_STREAM, currentStream, position, volume, 1.0f);
            soundManager.play(sound);
        }
    }

    public void playSound(SpeakerPosition position, ResourceLocation location, float volume, float pitch) {
        var soundManager = Minecraft.getInstance().getSoundManager();
        currentStream = null;

        if (sound != null) {
            soundManager.stop(sound);
            sound = null;
        }

        sound = new SpeakerSound(location, null, position, volume, pitch);
        soundManager.play(sound);
    }

    void setPosition(SpeakerPosition position) {
        if (sound != null) sound.setPosition(position);
    }

    void stop() {
        if (sound != null) Minecraft.getInstance().getSoundManager().stop(sound);

        currentStream = null;
        sound = null;
    }
}
