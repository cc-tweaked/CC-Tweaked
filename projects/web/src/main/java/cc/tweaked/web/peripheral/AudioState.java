// SPDX-FileCopyrightText: 2023 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package cc.tweaked.web.peripheral;

import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaTable;
import org.teavm.jso.webaudio.AudioBuffer;
import org.teavm.jso.webaudio.AudioContext;

import javax.annotation.Nullable;
import java.util.Optional;

import static cc.tweaked.web.peripheral.SpeakerPeripheral.SAMPLE_RATE;

final class AudioState {
    /**
     * The minimum size of the client's audio buffer. Once we have less than this on the client, we should send another
     * batch of audio.
     */
    private static final double CLIENT_BUFFER = 0.5;

    private final AudioContext audioContext;
    private @Nullable AudioBuffer nextBuffer;
    private double nextTime;

    AudioState(AudioContext audioContext) {
        this.audioContext = audioContext;
        nextTime = audioContext.getCurrentTime();
    }

    boolean pushBuffer(LuaTable<?, ?> table, int size, Optional<Double> volume) throws LuaException {
        if (nextBuffer != null) return false;

        var buffer = nextBuffer = audioContext.createBuffer(1, size, SAMPLE_RATE);
        var contents = buffer.getChannelData(0);

        for (var i = 0; i < size; i++) contents.set(i, table.getInt(i + 1) / 128.0f);

        // So we really should go via DFPWM here, but I do not have enough faith in our performance to do this properly.

        if (shouldSendPending()) playNext();
        return true;
    }

    boolean isPlaying() {
        return nextTime >= audioContext.getCurrentTime();
    }

    boolean shouldSendPending() {
        return nextBuffer != null && audioContext.getCurrentTime() >= nextTime - CLIENT_BUFFER;
    }

    void playNext() {
        if (nextBuffer == null) throw new NullPointerException("Buffer is null");
        var source = audioContext.createBufferSource();
        source.setBuffer(nextBuffer);
        source.connect(audioContext.getDestination());
        source.start(nextTime);

        nextTime += nextBuffer.getDuration();
        nextBuffer = null;
    }
}
