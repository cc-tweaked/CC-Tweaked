// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.network.client;

import dan200.computercraft.shared.command.text.TableBuilder;
import dan200.computercraft.shared.computer.core.ComputerState;
import dan200.computercraft.shared.computer.terminal.TerminalState;
import dan200.computercraft.shared.computer.upload.UploadResult;
import dan200.computercraft.shared.peripheral.speaker.SpeakerPosition;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;

import javax.annotation.Nullable;
import java.nio.ByteBuffer;
import java.util.UUID;

/**
 * The context under which clientbound packets are evaluated.
 */
public interface ClientNetworkContext {
    void handleChatTable(TableBuilder table);

    void handleComputerTerminal(int containerId, TerminalState terminal);

    void handleMonitorData(BlockPos pos, TerminalState terminal);

    void handlePlayRecord(BlockPos pos, @Nullable SoundEvent sound, @Nullable String name);

    void handlePocketComputerData(int instanceId, ComputerState state, int lightState, TerminalState terminal);

    void handlePocketComputerDeleted(int instanceId);

    void handleSpeakerAudio(UUID source, SpeakerPosition.Message position, float volume, ByteBuffer audio);

    void handleSpeakerMove(UUID source, SpeakerPosition.Message position);

    void handleSpeakerPlay(UUID source, SpeakerPosition.Message position, ResourceLocation sound, float volume, float pitch);

    void handleSpeakerStop(UUID source);

    void handleUploadResult(int containerId, UploadResult result, @Nullable Component errorMessage);
}
