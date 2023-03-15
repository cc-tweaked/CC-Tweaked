// SPDX-FileCopyrightText: 2021 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.client.sound;

import com.mojang.blaze3d.audio.Channel;
import dan200.computercraft.shared.peripheral.speaker.SpeakerPosition;
import net.minecraft.client.sounds.AudioStream;
import net.minecraft.client.sounds.SoundEngine;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Maps speakers source IDs to a {@link SpeakerInstance}.
 */
public class SpeakerManager {
    private static final Map<UUID, SpeakerInstance> sounds = new ConcurrentHashMap<>();

    public static void onPlayStreaming(SoundEngine engine, Channel channel, AudioStream stream) {
        if (!(stream instanceof DfpwmStream dfpwmStream)) return;

        // Associate the stream with the current channel, so SpeakerInstance.pushAudio can queue audio immediately.
        dfpwmStream.channel = channel;
        dfpwmStream.executor = engine.executor;
    }

    public static SpeakerInstance getSound(UUID source) {
        return sounds.computeIfAbsent(source, x -> new SpeakerInstance());
    }

    public static void stopSound(UUID source) {
        var sound = sounds.remove(source);
        if (sound != null) sound.stop();
    }

    public static void moveSound(UUID source, SpeakerPosition position) {
        var sound = sounds.get(source);
        if (sound != null) sound.setPosition(position);
    }

    public static void reset() {
        sounds.clear();
    }
}
