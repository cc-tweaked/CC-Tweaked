// SPDX-FileCopyrightText: 2018 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.network.client;

import dan200.computercraft.shared.network.NetworkMessage;
import dan200.computercraft.shared.peripheral.diskdrive.DiskDriveBlockEntity;
import dan200.computercraft.shared.platform.RegistryWrappers;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.sounds.SoundEvent;

import javax.annotation.Nullable;
import java.util.function.BiConsumer;

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
        soundEvent = buf.readBoolean() ? RegistryWrappers.readKey(buf, RegistryWrappers.SOUND_EVENTS) : null;
        name = buf.readBoolean() ? buf.readUtf(Short.MAX_VALUE) : null;
    }

    @Override
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        writeOptional(buf, soundEvent, (b, e) -> RegistryWrappers.writeKey(b, RegistryWrappers.SOUND_EVENTS, e));
        writeOptional(buf, name, FriendlyByteBuf::writeUtf);
    }

    @Override
    public void handle(ClientNetworkContext context) {
        context.handlePlayRecord(pos, soundEvent, name);
    }

    private static <T> void writeOptional(FriendlyByteBuf out, @Nullable T object, BiConsumer<FriendlyByteBuf, T> write) {
        if (object == null) {
            out.writeBoolean(false);
        } else {
            out.writeBoolean(true);
            write.accept(out, object);
        }
    }
}
