// SPDX-FileCopyrightText: 2021 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.client.sound;

import com.mojang.blaze3d.audio.Channel;
import dan200.computercraft.shared.peripheral.speaker.EncodedAudio;
import dan200.computercraft.shared.peripheral.speaker.SpeakerPeripheral;
import dan200.computercraft.shared.peripheral.speaker.SpeakerPosition;
import net.minecraft.client.sounds.AudioStream;
import net.minecraft.client.sounds.SoundEngine;
import org.jspecify.annotations.Nullable;
import org.lwjgl.BufferUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sound.sampled.AudioFormat;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.Executor;

/**
 * An {@link AudioStream} which decodes DFPWM streams, converting them to PCM.
 *
 * @see SpeakerPeripheral Server-side encoding of the audio.
 * @see SpeakerInstance
 */
class DfpwmStream implements AudioStream {
    private static final int PREC = 10;
    private static final int LPF_STRENGTH = 140;

    private static final AudioFormat MONO_8 = new AudioFormat(SpeakerPeripheral.SAMPLE_RATE, 8, 1, true, false);

    private static final Logger LOG = LoggerFactory.getLogger(DfpwmStream.class);
    private static final long DRIFT_THRESHOLD = (long) (0.5 * SpeakerPeripheral.SAMPLE_RATE); // 24000 samples = 500ms

    private final Queue<ByteBuffer> buffers = new ArrayDeque<>(2);

    /**
     * The {@link Channel} which this sound is playing on.
     *
     * @see SpeakerInstance#playAudio(SpeakerPosition, float, EncodedAudio)
     */
    @Nullable
    Channel channel;

    /**
     * The underlying {@link SoundEngine} executor.
     *
     * @see SpeakerInstance#playAudio(SpeakerPosition, float, EncodedAudio)
     * @see SoundEngine#executor
     */
    @Nullable
    Executor executor;

    private int lowPassCharge;

    private long consumedSamples = 0;
    private long bufferedSamples = 0;
    private long streamBaseOffset = 0;
    private boolean initialized = false;

    DfpwmStream() {
    }

    private ByteBuffer decodeDfpwm(EncodedAudio audio) {
        var charge = audio.charge();
        var strength = audio.strength();
        var previousBit = audio.previousBit();
        var input = audio.audio();

        var readable = input.remaining();
        var output = ByteBuffer.allocate(readable * 8).order(ByteOrder.nativeOrder());

        for (var i = 0; i < readable; i++) {
            var inputByte = input.get();
            for (var j = 0; j < 8; j++) {
                var currentBit = (inputByte & 1) != 0;
                var target = currentBit ? 127 : -128;

                // q' <- q + (s * (t - q) + 128)/256
                var nextCharge = charge + ((strength * (target - charge) + (1 << (PREC - 1))) >> PREC);
                if (nextCharge == charge && nextCharge != target) nextCharge += currentBit ? 1 : -1;

                var z = currentBit == previousBit ? (1 << PREC) - 1 : 0;

                var nextStrength = strength;
                if (strength != z) nextStrength += currentBit == previousBit ? 1 : -1;
                if (nextStrength < 2 << (PREC - 8)) nextStrength = 2 << (PREC - 8);

                // Apply antijerk
                var chargeWithAntijerk = currentBit == previousBit
                    ? nextCharge
                    : nextCharge + charge + 1 >> 1;

                // And low pass filter: outQ <- outQ + ((expectedOutput - outQ) x 140 / 256)
                lowPassCharge += ((chargeWithAntijerk - lowPassCharge) * LPF_STRENGTH + 0x80) >> 8;

                charge = nextCharge;
                strength = nextStrength;
                previousBit = currentBit;

                // OpenAL expects signed data ([0, 255]) while we produce unsigned ([-128, 127]). Do some bit twiddling
                // magic to convert.
                output.put((byte) ((lowPassCharge & 0xFF) ^ 0x80));

                inputByte >>= 1;
            }
        }

        output.flip();
        return output;
    }

    void push(EncodedAudio audio) {
        var decoded = decodeDfpwm(audio);
        var sampleCount = decoded.remaining();

        synchronized (this) {
            if (!initialized) {
                streamBaseOffset = audio.sampleOffset();
                consumedSamples = 0;
                bufferedSamples = 0;
                initialized = true;
                buffers.add(decoded);
                bufferedSamples += sampleCount;
                return;
            }

            var expectedServerOffset = streamBaseOffset + consumedSamples + bufferedSamples;
            var drift = audio.sampleOffset() - expectedServerOffset;

            if (drift > DRIFT_THRESHOLD) {
                // Client is behind — hard reset
                LOG.debug("Speaker audio hard reset: drift={} samples ({}s)", drift,
                    String.format("%.2f", (double) drift / SpeakerPeripheral.SAMPLE_RATE));
                buffers.clear();
                streamBaseOffset = audio.sampleOffset();
                consumedSamples = 0;
                bufferedSamples = 0;
                buffers.add(decoded);
                bufferedSamples += sampleCount;
            } else if (drift < -DRIFT_THRESHOLD) {
                // Packet is stale — drop it
                LOG.debug("Speaker audio dropping stale packet: drift={} samples", drift);
            } else {
                // Normal — enqueue
                buffers.add(decoded);
                bufferedSamples += sampleCount;
            }
        }
    }

    @Override
    public AudioFormat getFormat() {
        return MONO_8;
    }

    @Nullable
    @Override
    public synchronized ByteBuffer read(int capacity) {
        var result = BufferUtils.createByteBuffer(capacity);
        while (result.hasRemaining()) {
            var head = buffers.peek();
            if (head == null) break;

            var toRead = Math.min(head.remaining(), result.remaining());
            result.put(result.position(), head, head.position(), toRead);
            result.position(result.position() + toRead);
            head.position(head.position() + toRead);

            if (head.hasRemaining()) break;
            buffers.remove();
        }

        result.flip();

        // Track consumed samples for drift detection (MONO_8: 1 byte = 1 sample)
        var bytesRead = result.remaining();
        consumedSamples += bytesRead;
        bufferedSamples -= bytesRead;

        // This is naughty, but ensures we're not enqueuing empty buffers when the stream is exhausted.
        return bytesRead == 0 ? null : result;
    }

    @Override
    public void close() {
        synchronized (this) {
            buffers.clear();
            consumedSamples = 0;
            bufferedSamples = 0;
            initialized = false;
        }
    }

    public boolean isEmpty() {
        return buffers.isEmpty();
    }
}
