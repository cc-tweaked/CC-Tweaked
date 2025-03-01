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
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

/**
 * Starts a sound on the client.
 * <p>
 * Used by speakers to play sounds.
 *
 * @param source The {@linkplain SpeakerPeripheral#getSource() id} of the speaker playing audio.
 * @param pos    The position of the speaker.
 * @param sound  The sound to play.
 * @param volume The volume to play the sound at.
 * @param pitch  The pitch to play the sound at.
 * @see SpeakerBlockEntity
 */
public record SpeakerPlayClientMessage(
    UUID source,
    SpeakerPosition.Message pos,
    ResourceLocation sound,
    float volume,
    float pitch
) implements NetworkMessage<ClientNetworkContext> {
    public static final StreamCodec<RegistryFriendlyByteBuf, SpeakerPlayClientMessage> STREAM_CODEC = StreamCodec.composite(
        UUIDUtil.STREAM_CODEC, SpeakerPlayClientMessage::source,
        SpeakerPosition.Message.STREAM_CODEC, SpeakerPlayClientMessage::pos,
        ResourceLocation.STREAM_CODEC, SpeakerPlayClientMessage::sound,
        ByteBufCodecs.FLOAT, SpeakerPlayClientMessage::volume,
        ByteBufCodecs.FLOAT, SpeakerPlayClientMessage::pitch,
        SpeakerPlayClientMessage::new
    );

    public SpeakerPlayClientMessage(UUID source, SpeakerPosition pos, ResourceLocation sound, float volume, float pitch) {
        this(source, pos.asMessage(), sound, volume, pitch);
    }

    @Override
    public void handle(ClientNetworkContext context) {
        context.handleSpeakerPlay(source, pos, sound, volume, pitch);
    }

    @Override
    public CustomPacketPayload.Type<SpeakerPlayClientMessage> type() {
        return NetworkMessages.SPEAKER_PLAY;
    }
}
