// SPDX-FileCopyrightText: 2021 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.network.client;

import dan200.computercraft.shared.network.NetworkMessage;
import dan200.computercraft.shared.network.NetworkMessages;
import dan200.computercraft.shared.peripheral.speaker.SpeakerBlockEntity;
import dan200.computercraft.shared.peripheral.speaker.SpeakerPeripheral;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.UUID;

/**
 * Stops a sound on the client
 * <p>
 * Called when a speaker is broken.
 *
 * @param source The {@linkplain SpeakerPeripheral#getSource() id} of the speaker playing audio.
 * @see SpeakerBlockEntity
 */
public record SpeakerStopClientMessage(UUID source) implements NetworkMessage<ClientNetworkContext> {
    public static final StreamCodec<RegistryFriendlyByteBuf, SpeakerStopClientMessage> STREAM_CODEC = UUIDUtil.STREAM_CODEC
        .map(SpeakerStopClientMessage::new, SpeakerStopClientMessage::source)
        .cast();

    @Override
    public void handle(ClientNetworkContext context) {
        context.handleSpeakerStop(source);
    }

    @Override
    public CustomPacketPayload.Type<SpeakerStopClientMessage> type() {
        return NetworkMessages.SPEAKER_STOP;
    }
}
