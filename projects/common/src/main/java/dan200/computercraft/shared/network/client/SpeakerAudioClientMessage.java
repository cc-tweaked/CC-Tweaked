// SPDX-FileCopyrightText: 2021 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.network.client;

import dan200.computercraft.shared.network.NetworkMessage;
import dan200.computercraft.shared.network.NetworkMessages;
import dan200.computercraft.shared.peripheral.speaker.EncodedAudio;
import dan200.computercraft.shared.peripheral.speaker.SpeakerBlockEntity;
import dan200.computercraft.shared.peripheral.speaker.SpeakerPeripheral;
import dan200.computercraft.shared.peripheral.speaker.SpeakerPosition;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.UUID;

/**
 * Starts a sound on the client.
 * <p>
 * Used by speakers to play sounds.
 *
 * @param source  The {@linkplain SpeakerPeripheral#getSource() id} of the speaker playing audio.
 * @param pos     The position of the speaker.
 * @param content The audio to play.
 * @param volume  The volume to play the audio at.
 * @see SpeakerBlockEntity
 */
public record SpeakerAudioClientMessage(
    UUID source,
    SpeakerPosition.Message pos,
    EncodedAudio content,
    float volume
) implements NetworkMessage<ClientNetworkContext> {
    public static final StreamCodec<RegistryFriendlyByteBuf, SpeakerAudioClientMessage> STREAM_CODEC = StreamCodec.composite(
        UUIDUtil.STREAM_CODEC, SpeakerAudioClientMessage::source,
        SpeakerPosition.Message.STREAM_CODEC, SpeakerAudioClientMessage::pos,
        EncodedAudio.STREAM_CODEC, SpeakerAudioClientMessage::content,
        ByteBufCodecs.FLOAT, SpeakerAudioClientMessage::volume,
        SpeakerAudioClientMessage::new
    );

    public SpeakerAudioClientMessage(UUID source, SpeakerPosition pos, float volume, EncodedAudio content) {
        this(source, pos.asMessage(), content, volume);
    }

    @Override
    public void handle(ClientNetworkContext context) {
        context.handleSpeakerAudio(source, pos, volume, content);
    }

    @Override
    public CustomPacketPayload.Type<SpeakerAudioClientMessage> type() {
        return NetworkMessages.SPEAKER_AUDIO;
    }
}
