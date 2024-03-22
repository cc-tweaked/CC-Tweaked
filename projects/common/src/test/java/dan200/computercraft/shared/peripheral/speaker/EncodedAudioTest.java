// SPDX-FileCopyrightText: 2023 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.peripheral.speaker;

import dan200.computercraft.test.core.ArbitraryByteBuffer;
import io.netty.buffer.Unpooled;
import net.jqwik.api.*;
import net.minecraft.network.FriendlyByteBuf;

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

    @Provide
    Arbitrary<EncodedAudio> audio() {
        return Combinators.combine(
            Arbitraries.integers(),
            Arbitraries.integers(),
            Arbitraries.of(true, false),
            ArbitraryByteBuffer.bytes().ofMaxSize(1000)
        ).as(EncodedAudio::new);
    }
}
