// SPDX-FileCopyrightText: 2023 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.network.client;

import dan200.computercraft.test.core.StructuralEquality;
import dan200.computercraft.test.shared.MinecraftArbitraries;
import dan200.computercraft.test.shared.WithMinecraft;
import io.netty.buffer.Unpooled;
import net.jqwik.api.*;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.sounds.SoundEvent;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

@WithMinecraft
class PlayRecordClientMessageTest {
    static {
        WithMinecraft.Setup.bootstrap(); // @Property doesn't run test lifecycle methods.
    }

    /**
     * Sends packets on a roundtrip, ensuring that their contents are reassembled on the other end.
     *
     * @param message The message to send.
     */
    @Property
    public void testRoundTrip(@ForAll("message") PlayRecordClientMessage message) {
        var buffer = new FriendlyByteBuf(Unpooled.directBuffer());
        message.write(buffer);

        var converted = new PlayRecordClientMessage(buffer);
        assertEquals(buffer.readableBytes(), 0, "Whole packet was read");

        assertThat("Messages are equal", converted, equality.asMatcher(PlayRecordClientMessage.class, message));
    }

    @Provide
    Arbitrary<PlayRecordClientMessage> message() {
        return Combinators.combine(
            MinecraftArbitraries.blockPos(),
            MinecraftArbitraries.soundEvent().injectNull(0.3),
            Arbitraries.strings().ofMaxLength(1000).injectNull(0.3)
        ).as(PlayRecordClientMessage::new);
    }

    private static final StructuralEquality<PlayRecordClientMessage> equality = StructuralEquality.all(
        StructuralEquality.field(PlayRecordClientMessage.class, "pos"),
        StructuralEquality.field(PlayRecordClientMessage.class, "name"),
        StructuralEquality.field(PlayRecordClientMessage.class, "soundEvent", StructuralEquality.all(
            StructuralEquality.at("location", SoundEvent::getLocation),
            StructuralEquality.field(SoundEvent.class, "range"),
            StructuralEquality.field(SoundEvent.class, "newSystem")
        ))
    );
}
