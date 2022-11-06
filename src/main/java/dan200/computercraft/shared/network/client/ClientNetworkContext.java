/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.network.client;

import dan200.computercraft.impl.Services;
import dan200.computercraft.shared.command.text.TableBuilder;
import dan200.computercraft.shared.computer.core.ComputerState;
import dan200.computercraft.shared.computer.terminal.TerminalState;
import dan200.computercraft.shared.computer.upload.UploadResult;
import dan200.computercraft.shared.peripheral.speaker.SpeakerPosition;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * The context under which clientbound packets are evaluated.
 */
public interface ClientNetworkContext {
    static ClientNetworkContext get() {
        var instance = Instance.INSTANCE;
        return instance == null ? Services.raise(ClientNetworkContext.class, Instance.ERROR) : instance;
    }

    void handleChatTable(TableBuilder table);

    void handleComputerTerminal(int containerId, TerminalState terminal);

    void handleMonitorData(BlockPos pos, TerminalState terminal);

    void handlePlayRecord(BlockPos pos, @Nullable SoundEvent sound, @Nullable String name);

    void handlePocketComputerData(int instanceId, ComputerState state, int lightState, TerminalState terminal);

    void handlePocketComputerDeleted(int instanceId);

    void handleSpeakerAudio(UUID source, SpeakerPosition.Message position, float volume);

    void handleSpeakerAudioPush(UUID source, ByteBuf buffer);

    void handleSpeakerMove(UUID source, SpeakerPosition.Message position);

    void handleSpeakerPlay(UUID source, SpeakerPosition.Message position, ResourceLocation sound, float volume, float pitch);

    void handleSpeakerStop(UUID source);

    void handleUploadResult(int containerId, UploadResult result, Component errorMessage);

    final class Instance {
        static final @Nullable ClientNetworkContext INSTANCE;
        static final @Nullable Throwable ERROR;

        static {
            var helper = Services.tryLoad(ClientNetworkContext.class);
            INSTANCE = helper.instance();
            ERROR = helper.error();
        }

        private Instance() {
        }
    }
}
