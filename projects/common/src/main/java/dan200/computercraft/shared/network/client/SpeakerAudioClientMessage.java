// SPDX-FileCopyrightText: 2021 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.network.client;

import dan200.computercraft.shared.network.MessageType;
import dan200.computercraft.shared.network.NetworkMessage;
import dan200.computercraft.shared.network.NetworkMessages;
import dan200.computercraft.shared.peripheral.speaker.EncodedAudio;
import dan200.computercraft.shared.peripheral.speaker.SpeakerBlockEntity;
import dan200.computercraft.shared.peripheral.speaker.SpeakerPosition;
import net.minecraft.network.FriendlyByteBuf;

import java.util.UUID;

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
    private final EncodedAudio content;
    private final float volume;

    public SpeakerAudioClientMessage(UUID source, SpeakerPosition pos, float volume, EncodedAudio content) {
        this.source = source;
        this.pos = pos.asMessage();
        this.content = content;
        this.volume = volume;
    }

    public SpeakerAudioClientMessage(FriendlyByteBuf buf) {
        source = buf.readUUID();
        pos = SpeakerPosition.Message.read(buf);
        volume = buf.readFloat();
        content = EncodedAudio.read(buf);
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeUUID(source);
        pos.write(buf);
        buf.writeFloat(volume);
        content.write(buf);
    }

    @Override
    public void handle(ClientNetworkContext context) {
        context.handleSpeakerAudio(source, pos, volume, content);
    }

    @Override
    public MessageType<SpeakerAudioClientMessage> type() {
        return NetworkMessages.SPEAKER_AUDIO;
    }
}
