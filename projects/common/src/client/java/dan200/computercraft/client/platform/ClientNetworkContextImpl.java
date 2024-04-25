// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.client.platform;

import com.google.auto.service.AutoService;
import dan200.computercraft.client.ClientTableFormatter;
import dan200.computercraft.client.gui.AbstractComputerScreen;
import dan200.computercraft.client.gui.OptionScreen;
import dan200.computercraft.client.pocket.ClientPocketComputers;
import dan200.computercraft.client.sound.SpeakerManager;
import dan200.computercraft.shared.command.text.TableBuilder;
import dan200.computercraft.shared.computer.core.ComputerState;
import dan200.computercraft.shared.computer.menu.ComputerMenu;
import dan200.computercraft.shared.computer.terminal.TerminalState;
import dan200.computercraft.shared.computer.upload.UploadResult;
import dan200.computercraft.shared.network.client.ClientNetworkContext;
import dan200.computercraft.shared.peripheral.monitor.MonitorBlockEntity;
import dan200.computercraft.shared.peripheral.speaker.EncodedAudio;
import dan200.computercraft.shared.peripheral.speaker.SpeakerPosition;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * The client-side implementation of {@link ClientNetworkContext}.
 */
@AutoService(ClientNetworkContext.class)
public final class ClientNetworkContextImpl implements ClientNetworkContext {
    @Override
    public void handleChatTable(TableBuilder table) {
        ClientTableFormatter.INSTANCE.display(table);
    }

    @Override
    public void handleComputerTerminal(int containerId, TerminalState terminal) {
        Player player = Minecraft.getInstance().player;
        if (player != null && player.containerMenu.containerId == containerId && player.containerMenu instanceof ComputerMenu menu) {
            menu.updateTerminal(terminal);
        }
    }

    @Override
    public void handleMonitorData(BlockPos pos, @Nullable TerminalState terminal) {
        var player = Minecraft.getInstance().player;
        if (player == null) return;

        var te = player.level().getBlockEntity(pos);
        if (!(te instanceof MonitorBlockEntity monitor)) return;

        monitor.read(terminal);
    }

    @Override
    public void handlePlayRecord(BlockPos pos, @Nullable SoundEvent sound, @Nullable String name) {
        var mc = Minecraft.getInstance();
        ClientPlatformHelper.get().playStreamingMusic(pos, sound);
        if (name != null) mc.gui.setNowPlaying(Component.literal(name));
    }

    @Override
    public void handlePocketComputerData(UUID instanceId, ComputerState state, int lightState, @Nullable TerminalState terminal) {
        ClientPocketComputers.setState(instanceId, state, lightState, terminal);
    }

    @Override
    public void handlePocketComputerDeleted(UUID instanceId) {
        ClientPocketComputers.remove(instanceId);
    }

    @Override
    public void handleSpeakerAudio(UUID source, SpeakerPosition.Message position, float volume, EncodedAudio buffer) {
        SpeakerManager.getSound(source).playAudio(reifyPosition(position), volume, buffer);
    }

    @Override
    public void handleSpeakerMove(UUID source, SpeakerPosition.Message position) {
        SpeakerManager.moveSound(source, reifyPosition(position));
    }

    @Override
    public void handleSpeakerPlay(UUID source, SpeakerPosition.Message position, ResourceLocation sound, float volume, float pitch) {
        SpeakerManager.getSound(source).playSound(reifyPosition(position), sound, volume, pitch);
    }

    @Override
    public void handleSpeakerStop(UUID source) {
        SpeakerManager.stopSound(source);
    }

    @Override
    public void handleUploadResult(int containerId, UploadResult result, @Nullable Component errorMessage) {
        var minecraft = Minecraft.getInstance();

        var screen = OptionScreen.unwrap(minecraft.screen);
        if (screen instanceof AbstractComputerScreen<?> && ((AbstractComputerScreen<?>) screen).getMenu().containerId == containerId) {
            ((AbstractComputerScreen<?>) screen).uploadResult(result, errorMessage);
        }
    }

    private static SpeakerPosition reifyPosition(SpeakerPosition.Message pos) {
        var minecraft = Minecraft.getInstance();
        Level level = minecraft.level;
        if (level != null && !level.dimension().location().equals(pos.level())) level = null;

        return new SpeakerPosition(
            level, pos.position(),
            level != null && pos.entity().isPresent() ? level.getEntity(pos.entity().getAsInt()) : null
        );
    }
}
