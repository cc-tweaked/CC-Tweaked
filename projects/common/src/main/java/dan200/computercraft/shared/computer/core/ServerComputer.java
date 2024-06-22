// Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
//
// SPDX-License-Identifier: LicenseRef-CCPL

package dan200.computercraft.shared.computer.core;

import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.api.filesystem.WritableMount;
import dan200.computercraft.api.lua.ILuaAPI;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.core.apis.IAPIEnvironment;
import dan200.computercraft.core.computer.Computer;
import dan200.computercraft.core.computer.ComputerEnvironment;
import dan200.computercraft.core.computer.ComputerSide;
import dan200.computercraft.core.metrics.MetricsObserver;
import dan200.computercraft.shared.computer.apis.CommandAPI;
import dan200.computercraft.shared.computer.menu.ComputerMenu;
import dan200.computercraft.shared.computer.terminal.NetworkedTerminal;
import dan200.computercraft.shared.computer.terminal.TerminalState;
import dan200.computercraft.shared.config.Config;
import dan200.computercraft.shared.network.NetworkMessage;
import dan200.computercraft.shared.network.client.ClientNetworkContext;
import dan200.computercraft.shared.network.client.ComputerTerminalClientMessage;
import dan200.computercraft.shared.network.server.ServerNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;

import javax.annotation.Nullable;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

public class ServerComputer implements InputHandler, ComputerEnvironment {
    private final int instanceID;
    private final UUID instanceUUID = UUID.randomUUID();

    private ServerLevel level;
    private BlockPos position;

    private final ComputerFamily family;
    private final MetricsObserver metrics;
    private final Computer computer;

    private final NetworkedTerminal terminal;
    private final AtomicBoolean terminalChanged = new AtomicBoolean(false);

    private int ticksSincePing;

    public ServerComputer(
        ServerLevel level, BlockPos position, int computerID, @Nullable String label, ComputerFamily family, int terminalWidth, int terminalHeight
    ) {
        this.level = level;
        this.position = position;
        this.family = family;

        var context = ServerContext.get(level.getServer());
        instanceID = context.registry().getUnusedInstanceID();
        terminal = new NetworkedTerminal(terminalWidth, terminalHeight, family != ComputerFamily.NORMAL, this::markTerminalChanged);
        metrics = context.metrics().createMetricObserver(this);

        computer = new Computer(context.computerContext(), this, terminal, computerID);
        computer.setLabel(label);

        if (family == ComputerFamily.COMMAND) addAPI(new CommandAPI(this));
    }

    public ComputerFamily getFamily() {
        return family;
    }

    public ServerLevel getLevel() {
        return level;
    }

    public void setLevel(ServerLevel level) {
        this.level = level;
    }

    public BlockPos getPosition() {
        return position;
    }

    public void setPosition(BlockPos pos) {
        position = new BlockPos(pos);
    }

    public IAPIEnvironment getAPIEnvironment() {
        return computer.getAPIEnvironment();
    }

    public Computer getComputer() {
        return computer;
    }

    protected void markTerminalChanged() {
        terminalChanged.set(true);
    }

    public void tickServer() {
        ticksSincePing++;
        computer.tick();
        if (terminalChanged.getAndSet(false)) onTerminalChanged();
    }

    protected void onTerminalChanged() {
        sendToAllInteracting(c -> new ComputerTerminalClientMessage(c, getTerminalState()));
    }

    public TerminalState getTerminalState() {
        return new TerminalState(terminal);
    }

    public void keepAlive() {
        ticksSincePing = 0;
    }

    public boolean hasTimedOut() {
        return ticksSincePing > 100;
    }

    public int pollAndResetChanges() {
        return computer.pollAndResetChanges();
    }

    public UUID register() {
        ServerContext.get(level.getServer()).registry().add(this);
        return instanceUUID;
    }

    void unload() {
        computer.unload();
    }

    public void close() {
        unload();
        ServerContext.get(level.getServer()).registry().remove(this);
    }

    /**
     * Check whether this computer is usable by a player.
     *
     * @param player The player trying to use this computer.
     * @return Whether this computer can be used.
     */
    public final boolean checkUsable(Player player) {
        return ServerContext.get(level.getServer()).registry().get(instanceUUID) == this
            && getFamily().checkUsable(player);
    }

    private void sendToAllInteracting(Function<AbstractContainerMenu, NetworkMessage<ClientNetworkContext>> createPacket) {
        var server = level.getServer();

        for (var player : server.getPlayerList().getPlayers()) {
            if (player.containerMenu instanceof ComputerMenu && ((ComputerMenu) player.containerMenu).getComputer() == this) {
                ServerNetworking.sendToPlayer(createPacket.apply(player.containerMenu), player);
            }
        }
    }

    protected void onRemoved() {
    }

    public int getInstanceID() {
        return instanceID;
    }

    public UUID getInstanceUUID() {
        return instanceUUID;
    }

    public int getID() {
        return computer.getID();
    }

    public @Nullable String getLabel() {
        return computer.getLabel();
    }

    public boolean isOn() {
        return computer.isOn();
    }

    public ComputerState getState() {
        if (!computer.isOn()) return ComputerState.OFF;
        return computer.isBlinking() ? ComputerState.BLINKING : ComputerState.ON;
    }

    @Override
    public void turnOn() {
        // Turn on
        computer.turnOn();
    }

    @Override
    public void shutdown() {
        // Shutdown
        computer.shutdown();
    }

    @Override
    public void reboot() {
        // Reboot
        computer.reboot();
    }

    @Override
    public void queueEvent(String event, @Nullable Object[] arguments) {
        // Queue event
        computer.queueEvent(event, arguments);
    }

    public int getRedstoneOutput(ComputerSide side) {
        return computer.getEnvironment().getExternalRedstoneOutput(side);
    }

    public void setRedstoneInput(ComputerSide side, int level) {
        computer.getEnvironment().setRedstoneInput(side, level);
    }

    public int getBundledRedstoneOutput(ComputerSide side) {
        return computer.getEnvironment().getExternalBundledRedstoneOutput(side);
    }

    public void setBundledRedstoneInput(ComputerSide side, int combination) {
        computer.getEnvironment().setBundledRedstoneInput(side, combination);
    }

    public void addAPI(ILuaAPI api) {
        computer.addApi(api);
    }

    public void setPeripheral(ComputerSide side, @Nullable IPeripheral peripheral) {
        computer.getEnvironment().setPeripheral(side, peripheral);
    }

    @Nullable
    public IPeripheral getPeripheral(ComputerSide side) {
        return computer.getEnvironment().getPeripheral(side);
    }

    public void setLabel(@Nullable String label) {
        computer.setLabel(label);
    }

    @Override
    public double getTimeOfDay() {
        return (level.getDayTime() + 6000) % 24000 / 1000.0;
    }

    @Override
    public int getDay() {
        return (int) ((level.getDayTime() + 6000) / 24000) + 1;
    }

    @Override
    public MetricsObserver getMetrics() {
        return metrics;
    }

    @Override
    public @Nullable WritableMount createRootMount() {
        return ComputerCraftAPI.createSaveDirMount(level.getServer(), "computer/" + computer.getID(), Config.computerSpaceLimit);
    }
}
