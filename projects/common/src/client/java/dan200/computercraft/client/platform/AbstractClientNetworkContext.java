/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.client.platform;

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
import dan200.computercraft.shared.peripheral.speaker.SpeakerPosition;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * The base implementation of {@link ClientNetworkContext}.
 * <p>
 * This should be extended by mod loader specific modules with the remaining abstract methods.
 */
public abstract class AbstractClientNetworkContext implements ClientNetworkContext {
    @Override
    public final void handleChatTable(TableBuilder table) {
        ClientTableFormatter.INSTANCE.display(table);
    }

    @Override
    public final void handleComputerTerminal(int containerId, TerminalState terminal) {
        Player player = Minecraft.getInstance().player;
        if (player != null && player.containerMenu.containerId == containerId && player.containerMenu instanceof ComputerMenu menu) {
            menu.updateTerminal(terminal);
        }
    }

    @Override
    public final void handleMonitorData(BlockPos pos, TerminalState terminal) {
        var player = Minecraft.getInstance().player;
        if (player == null) return;

        var te = player.level.getBlockEntity(pos);
        if (!(te instanceof MonitorBlockEntity monitor)) return;

        monitor.read(terminal);
    }

    @Override
    public final void handlePocketComputerData(int instanceId, ComputerState state, int lightState, TerminalState terminal) {
        var computer = ClientPocketComputers.get(instanceId, terminal.colour);
        computer.setState(state, lightState);
        if (terminal.hasTerminal()) computer.setTerminal(terminal);
    }

    @Override
    public final void handlePocketComputerDeleted(int instanceId) {
        ClientPocketComputers.remove(instanceId);
    }

    @Override
    public final void handleSpeakerAudio(UUID source, SpeakerPosition.Message position, float volume) {
        SpeakerManager.getSound(source).playAudio(reifyPosition(position), volume);
    }

    @Override
    public final void handleSpeakerAudioPush(UUID source, ByteBuf buffer) {
        SpeakerManager.getSound(source).pushAudio(buffer);
    }

    @Override
    public final void handleSpeakerMove(UUID source, SpeakerPosition.Message position) {
        SpeakerManager.moveSound(source, reifyPosition(position));
    }

    @Override
    public final void handleSpeakerPlay(UUID source, SpeakerPosition.Message position, ResourceLocation sound, float volume, float pitch) {
        SpeakerManager.getSound(source).playSound(reifyPosition(position), sound, volume, pitch);
    }

    @Override
    public final void handleSpeakerStop(UUID source) {
        SpeakerManager.stopSound(source);
    }

    @Override
    public final void handleUploadResult(int containerId, UploadResult result, @Nullable Component errorMessage) {
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
