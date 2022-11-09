/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.network.client;

import dan200.computercraft.shared.network.NetworkMessage;
import dan200.computercraft.shared.platform.Registries;
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
 * @see dan200.computercraft.shared.peripheral.diskdrive.TileDiskDrive
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
        soundEvent = buf.readBoolean() ? Registries.readKey(buf, Registries.SOUND_EVENTS) : null;
        name = buf.readBoolean() ? buf.readUtf(Short.MAX_VALUE) : null;
    }

    @Override
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        writeOptional(buf, soundEvent, (b, e) -> Registries.writeKey(b, Registries.SOUND_EVENTS, e));
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
