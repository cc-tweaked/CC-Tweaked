// SPDX-FileCopyrightText: 2018 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.network.client;

import dan200.computercraft.shared.network.MessageType;
import dan200.computercraft.shared.network.NetworkMessage;
import dan200.computercraft.shared.network.NetworkMessages;
import dan200.computercraft.shared.peripheral.diskdrive.DiskDriveBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.sounds.SoundEvent;

import javax.annotation.Nullable;

/**
 * Starts or stops a record on the client, depending on if {@link #soundEvent} is {@code null}.
 * <p>
 * Used by disk drives to play record items.
 *
 * @see DiskDriveBlockEntity
 */
public class PlayRecordClientMessage implements NetworkMessage<ClientNetworkContext> {
    private final BlockPos pos;
    private final @Nullable String name;
    private final @Nullable SoundEvent soundEvent;

    public PlayRecordClientMessage(BlockPos pos, SoundEvent event, @Nullable String name) {
        this.pos = pos;
        this.name = name;
        soundEvent = event;
    }

    public PlayRecordClientMessage(BlockPos pos) {
        this.pos = pos;
        name = null;
        soundEvent = null;
    }

    public PlayRecordClientMessage(FriendlyByteBuf buf) {
        pos = buf.readBlockPos();
        soundEvent = buf.readNullable(SoundEvent::readFromNetwork);
        name = buf.readNullable(FriendlyByteBuf::readUtf);
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeNullable(soundEvent, (b, e) -> e.writeToNetwork(b));
        buf.writeNullable(name, FriendlyByteBuf::writeUtf);
    }

    @Override
    public void handle(ClientNetworkContext context) {
        context.handlePlayRecord(pos, soundEvent, name);
    }

    @Override
    public MessageType<PlayRecordClientMessage> type() {
        return NetworkMessages.PLAY_RECORD;
    }
}
