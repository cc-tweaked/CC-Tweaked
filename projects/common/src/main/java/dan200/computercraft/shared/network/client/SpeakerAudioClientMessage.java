// SPDX-FileCopyrightText: 2021 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.network.client;

import dan200.computercraft.shared.network.NetworkMessage;
import dan200.computercraft.shared.peripheral.speaker.SpeakerBlockEntity;
import dan200.computercraft.shared.peripheral.speaker.SpeakerPosition;
import net.minecraft.network.FriendlyByteBuf;

import javax.annotation.Nullable;
import java.nio.ByteBuffer;
import java.util.UUID;

import static dan200.computercraft.core.util.Nullability.assertNonNull;

/**
 * Starts a sound on the client.
 * <p>
 * Used by speakers to play sounds.
 *
 * @see SpeakerBlockEntity
 */
public class SpeakerAudioClientMessage implements NetworkMessage<ClientNetworkContext> {
    private final UUID source;
    private final SpeakerPosition.Message pos;
    private final @Nullable ByteBuffer content;
    private final float volume;
    private final boolean isPCM;

    public SpeakerAudioClientMessage(UUID source, SpeakerPosition pos, float volume, ByteBuffer content, boolean isPCM) {
        this.source = source;
        this.pos = pos.asMessage();
        this.content = content;
        this.volume = volume;
        this.isPCM = isPCM;
    }

    public SpeakerAudioClientMessage(FriendlyByteBuf buf) {
        source = buf.readUUID();
        pos = SpeakerPosition.Message.read(buf);
        volume = buf.readFloat();
        isPCM = buf.readByte() != 0;

        // TODO: Remove this, so we no longer need a getter for ClientNetworkContext. However, doing so without
        //  leaking or redundantly copying the buffer is hard.
        ClientNetworkContext.get().handleSpeakerAudioPush(source, buf, isPCM);
        content = null;
    }

    @Override
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(source);
        pos.write(buf);
        buf.writeFloat(volume);
        buf.writeByte(isPCM ? 1 : 0);
        buf.writeBytes(assertNonNull(content).duplicate());
    }

    @Override
    public void handle(ClientNetworkContext context) {
        context.handleSpeakerAudio(source, pos, volume);
    }
}
