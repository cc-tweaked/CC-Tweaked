// SPDX-FileCopyrightText: 2021 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.network.client;

import dan200.computercraft.shared.network.MessageType;
import dan200.computercraft.shared.network.NetworkMessage;
import dan200.computercraft.shared.network.NetworkMessages;
import dan200.computercraft.shared.peripheral.speaker.SpeakerBlockEntity;
import net.minecraft.network.FriendlyByteBuf;

import java.util.UUID;

/**
 * Stops a sound on the client
 * <p>
 * Called when a speaker is broken.
 *
 * @see SpeakerBlockEntity
 */
public class SpeakerStopClientMessage implements NetworkMessage<ClientNetworkContext> {
    private final UUID source;

    public SpeakerStopClientMessage(UUID source) {
        this.source = source;
    }

    public SpeakerStopClientMessage(FriendlyByteBuf buf) {
        source = buf.readUUID();
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeUUID(source);
    }

    @Override
    public void handle(ClientNetworkContext context) {
        context.handleSpeakerStop(source);
    }

    @Override
    public MessageType<SpeakerStopClientMessage> type() {
        return NetworkMessages.SPEAKER_STOP;
    }
}
