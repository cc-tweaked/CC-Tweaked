// SPDX-FileCopyrightText: 2024 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.peripheral.speaker;

import net.minecraft.network.FriendlyByteBuf;

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
    public void write(FriendlyByteBuf buf) {
        buf.writeVarInt(charge());
        buf.writeVarInt(strength());
        buf.writeBoolean(previousBit());
        buf.writeVarInt(audio.remaining());
        buf.writeBytes(audio().duplicate());
    }

    public static EncodedAudio read(FriendlyByteBuf buf) {
        var charge = buf.readVarInt();
        var strength = buf.readVarInt();
        var previousBit = buf.readBoolean();

        var length = buf.readVarInt();
        var bytes = new byte[length];
        buf.readBytes(bytes);

        return new EncodedAudio(charge, strength, previousBit, ByteBuffer.wrap(bytes));
    }
}
