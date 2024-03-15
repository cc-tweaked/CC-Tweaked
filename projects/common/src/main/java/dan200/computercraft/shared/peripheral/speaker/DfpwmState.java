// SPDX-FileCopyrightText: 2021 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.peripheral.speaker;

import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaTable;
import dan200.computercraft.shared.util.PauseAwareTimer;

import javax.annotation.Nullable;
import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static dan200.computercraft.shared.peripheral.speaker.SpeakerPeripheral.SAMPLE_RATE;
import static dan200.computercraft.shared.peripheral.speaker.SpeakerPeripheral.clampVolume;

/**
 * Internal state of the DFPWM decoder and the state of playback.
 */
class DfpwmState {
    private static final long SECOND = TimeUnit.SECONDS.toNanos(1);

    /**
     * The minimum size of the client's audio buffer. Once we have less than this on the client, we should send another
     * batch of audio.
     */
    private static final long CLIENT_BUFFER = (long) (SECOND * 0.5);

    private static final int PREC = 10;

    private int charge = 0; // q
    private int strength = 0; // s
    private boolean previousBit = false;

    private boolean unplayed = true;
    private long clientEndTime = PauseAwareTimer.getTime();
    private float pendingVolume = 1.0f;
    private @Nullable EncodedAudio pendingAudio;

    synchronized boolean pushBuffer(LuaTable<?, ?> table, int size, Optional<Double> volume) throws LuaException {
        if (pendingAudio != null) return false;

        var outSize = size / 8;
        var buffer = ByteBuffer.allocate(outSize);

        var initialCharge = charge;
        var initialStrength = strength;
        var initialPreviousBit = previousBit;

        for (var i = 0; i < outSize; i++) {
            var thisByte = 0;
            for (var j = 1; j <= 8; j++) {
                var level = table.getInt(i * 8 + j);
                if (level < -128 || level > 127) {
                    throw new LuaException("table item #" + (i * 8 + j) + " must be between -128 and 127");
                }

                var currentBit = level > charge || (level == charge && charge == 127);

                // Identical to DfpwmStream. Not happy with this, but saves some inheritance.
                var target = currentBit ? 127 : -128;

                // q' <- q + (s * (t - q) + 128)/256
                var nextCharge = charge + ((strength * (target - charge) + (1 << (PREC - 1))) >> PREC);
                if (nextCharge == charge && nextCharge != target) nextCharge += currentBit ? 1 : -1;

                var z = currentBit == previousBit ? (1 << PREC) - 1 : 0;

                var nextStrength = strength;
                if (strength != z) nextStrength += currentBit == previousBit ? 1 : -1;
                if (nextStrength < 2 << (PREC - 8)) nextStrength = 2 << (PREC - 8);

                charge = nextCharge;
                strength = nextStrength;
                previousBit = currentBit;

                thisByte = (thisByte >> 1) + (currentBit ? 128 : 0);
            }

            buffer.put((byte) thisByte);
        }

        buffer.flip();

        pendingAudio = new EncodedAudio(initialCharge, initialStrength, initialPreviousBit, buffer);
        pendingVolume = (float) clampVolume(volume.orElse((double) pendingVolume));
        return true;
    }

    boolean shouldSendPending(long now) {
        return pendingAudio != null && now >= clientEndTime - CLIENT_BUFFER;
    }

    EncodedAudio pullPending(long now) {
        var audio = pendingAudio;
        if (audio == null) throw new IllegalStateException("Should not pull pending audio yet");
        pendingAudio = null;
        // Compute when we should consider sending the next packet.
        clientEndTime = Math.max(now, clientEndTime) + (audio.audio().remaining() * SECOND * 8 / SAMPLE_RATE);
        unplayed = false;
        return audio;
    }

    boolean isPlaying() {
        return unplayed || clientEndTime >= PauseAwareTimer.getTime();
    }

    float getVolume() {
        return pendingVolume;
    }
}
