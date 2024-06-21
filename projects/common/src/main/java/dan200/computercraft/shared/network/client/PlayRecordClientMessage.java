// SPDX-FileCopyrightText: 2018 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.network.client;

import dan200.computercraft.shared.network.NetworkMessage;
import dan200.computercraft.shared.network.NetworkMessages;
import dan200.computercraft.shared.peripheral.diskdrive.DiskDriveBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.JukeboxSong;

import java.util.Optional;

/**
 * Starts or stops a record on the client, depending on if {@link #song} is {@code null}.
 * <p>
 * Used by disk drives to play record items.
 *
 * @param pos  The position of the speaker, where we should play this sound.
 * @param song The sound to play, or {@link Optional#empty()} if we should stop playing.
 * @param name The title of the audio to play.
 * @see DiskDriveBlockEntity
 */
public record PlayRecordClientMessage(
    BlockPos pos, Optional<Holder<JukeboxSong>> song
) implements NetworkMessage<ClientNetworkContext> {
    public static final StreamCodec<RegistryFriendlyByteBuf, PlayRecordClientMessage> STREAM_CODEC = StreamCodec.composite(
        BlockPos.STREAM_CODEC, PlayRecordClientMessage::pos,
        ByteBufCodecs.optional(JukeboxSong.STREAM_CODEC), PlayRecordClientMessage::song,
        PlayRecordClientMessage::new
    );

    public PlayRecordClientMessage(BlockPos pos) {
        this(pos, Optional.empty());
    }

    @Override
    public void handle(ClientNetworkContext context) {
        context.handlePlayRecord(pos, song.orElse(null));
    }

    @Override
    public CustomPacketPayload.Type<PlayRecordClientMessage> type() {
        return NetworkMessages.PLAY_RECORD;
    }
}
