// SPDX-FileCopyrightText: 2021 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.network.client;

import dan200.computercraft.shared.network.NetworkMessage;
import dan200.computercraft.shared.network.NetworkMessages;
import dan200.computercraft.shared.peripheral.speaker.SpeakerBlockEntity;
import dan200.computercraft.shared.peripheral.speaker.SpeakerPeripheral;
import dan200.computercraft.shared.peripheral.speaker.SpeakerPosition;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.UUID;

/**
 * Starts a sound on the client.
 * <p>
 * Used by speakers to play sounds.
 *
 * @param source The {@linkplain SpeakerPeripheral#getSource() id} of the speaker playing audio.
 * @param pos    The new position of the speaker.
 * @see SpeakerBlockEntity
 */
public record SpeakerMoveClientMessage(
    UUID source, SpeakerPosition.Message pos
) implements NetworkMessage<ClientNetworkContext> {
    public static final StreamCodec<RegistryFriendlyByteBuf, SpeakerMoveClientMessage> STREAM_CODEC = StreamCodec.composite(
        UUIDUtil.STREAM_CODEC, SpeakerMoveClientMessage::source,
        SpeakerPosition.Message.STREAM_CODEC, SpeakerMoveClientMessage::pos,
        SpeakerMoveClientMessage::new
    );

    public SpeakerMoveClientMessage(UUID source, SpeakerPosition pos) {
        this(source, pos.asMessage());
    }

    @Override
    public void handle(ClientNetworkContext context) {
        context.handleSpeakerMove(source, pos);
    }

    @Override
    public CustomPacketPayload.Type<SpeakerMoveClientMessage> type() {
        return NetworkMessages.SPEAKER_MOVE;
    }
}
