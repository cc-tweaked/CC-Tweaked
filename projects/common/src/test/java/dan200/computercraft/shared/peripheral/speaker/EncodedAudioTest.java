// SPDX-FileCopyrightText: 2023 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.peripheral.speaker;

import dan200.computercraft.test.core.ArbitraryByteBuffer;
import io.netty.buffer.Unpooled;
import net.jqwik.api.*;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;

class EncodedAudioTest {
    /**
     * Sends the audio on a roundtrip, ensuring that its contents are reassembled on the other end.
     *
     * @param audio The message to send.
     */
    @Property
    public void testRoundTrip(@ForAll("audio") EncodedAudio audio) {
        var buffer = new FriendlyByteBuf(Unpooled.directBuffer());
        audio.write(buffer);

        var converted = EncodedAudio.read(buffer);
        assertEquals(buffer.readableBytes(), 0, "Whole packet was read");

        assertThat("Messages are equal", converted, equalTo(converted));
    }

    @Test
    public void testSampleOffsetRoundTrip() {
        var audio = new EncodedAudio(10, 20, true, ByteBuffer.wrap(new byte[]{1, 2, 3, 4}), 48000L);
        var buf = new FriendlyByteBuf(Unpooled.buffer());
        audio.write(buf);
        var decoded = EncodedAudio.read(buf);
        assertEquals(48000L, decoded.sampleOffset());
        assertEquals(10, decoded.charge());
    }

    @Provide
    Arbitrary<EncodedAudio> audio() {
        return Combinators.combine(
            Arbitraries.integers(),
            Arbitraries.integers(),
            Arbitraries.of(true, false),
            ArbitraryByteBuffer.bytes().ofMaxSize(1000),
            Arbitraries.longs().between(0, 1_000_000_000L)
        ).as(EncodedAudio::new);
    }
}
