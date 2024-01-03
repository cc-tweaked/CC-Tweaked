// SPDX-FileCopyrightText: 2021 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.network.client;

import dan200.computercraft.shared.network.MessageType;
import dan200.computercraft.shared.network.NetworkMessage;
import dan200.computercraft.shared.network.NetworkMessages;
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
public class SpeakerMoveClientMessage implements NetworkMessage<ClientNetworkContext> {
    private final UUID source;
    private final SpeakerPosition.Message pos;

    public SpeakerMoveClientMessage(UUID source, SpeakerPosition pos) {
        this.source = source;
        this.pos = pos.asMessage();
    }

    public SpeakerMoveClientMessage(FriendlyByteBuf buf) {
        source = buf.readUUID();
        pos = SpeakerPosition.Message.read(buf);
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeUUID(source);
        pos.write(buf);
    }

    @Override
    public void handle(ClientNetworkContext context) {
        context.handleSpeakerMove(source, pos);
    }

    @Override
    public MessageType<SpeakerMoveClientMessage> type() {
        return NetworkMessages.SPEAKER_MOVE;
    }
}
