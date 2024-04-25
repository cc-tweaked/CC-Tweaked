// SPDX-FileCopyrightText: 2024 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.peripheral.speaker;

import dan200.computercraft.shared.network.codec.MoreStreamCodecs;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.nio.ByteBuffer;

/**
 * A chunk of encoded audio, along with the state required for the decoder to reproduce the original audio samples.
 *
 * @param charge      The DFPWM charge.
 * @param strength    The DFPWM strength.
 * @param previousBit The previous bit.
 * @param audio       The block of encoded audio.
 */
public record EncodedAudio(int charge, int strength, boolean previousBit, ByteBuffer audio) {
    public static final StreamCodec<ByteBuf, EncodedAudio> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT, EncodedAudio::charge,
        ByteBufCodecs.VAR_INT, EncodedAudio::strength,
        ByteBufCodecs.BOOL, EncodedAudio::previousBit,
        MoreStreamCodecs.BYTE_BUFFER, EncodedAudio::audio,
        EncodedAudio::new
    );
}
