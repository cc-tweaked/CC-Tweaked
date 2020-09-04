/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.network.client;

import javax.annotation.Nonnull;

import dan200.computercraft.shared.network.NetworkMessage;

import net.minecraft.client.MinecraftClient;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.LiteralText;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.Registry;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.network.PacketContext;

/**
 * Starts or stops a record on the client, depending on if {@link #soundEvent} is {@code null}.
 *
 * Used by disk drives to play record items.
 *
 * @see dan200.computercraft.shared.peripheral.diskdrive.TileDiskDrive
 */
public class PlayRecordClientMessage implements NetworkMessage {
    private final BlockPos pos;
    private final String name;
    private final SoundEvent soundEvent;

    public PlayRecordClientMessage(BlockPos pos, SoundEvent event, String name) {
        this.pos = pos;
        this.name = name;
        this.soundEvent = event;
    }

    public PlayRecordClientMessage(BlockPos pos) {
        this.pos = pos;
        this.name = null;
        this.soundEvent = null;
    }

    public PlayRecordClientMessage(PacketByteBuf buf) {
        this.pos = buf.readBlockPos();
        if (buf.readBoolean()) {
            this.name = buf.readString(Short.MAX_VALUE);
            this.soundEvent = Registry.SOUND_EVENT.get(buf.readIdentifier());
        } else {
            this.name = null;
            this.soundEvent = null;
        }
    }

    @Override
    public void toBytes(@Nonnull PacketByteBuf buf) {
        buf.writeBlockPos(this.pos);
        if (this.soundEvent == null) {
            buf.writeBoolean(false);
        } else {
            buf.writeBoolean(true);
            buf.writeString(this.name);
            buf.writeIdentifier(this.soundEvent.getId());
        }
    }

    @Override
    @Environment (EnvType.CLIENT)
    public void handle(PacketContext context) {
        MinecraftClient mc = MinecraftClient.getInstance();
        mc.worldRenderer.playSong(this.soundEvent, this.pos);
        if (this.name != null) {
            mc.inGameHud.setRecordPlayingOverlay(new LiteralText(this.name));
        }
    }
}
