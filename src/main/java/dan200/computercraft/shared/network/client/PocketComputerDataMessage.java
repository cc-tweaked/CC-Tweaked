/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.network.client;

import dan200.computercraft.client.pocket.ClientPocketComputers;
import dan200.computercraft.shared.computer.core.ComputerState;
import dan200.computercraft.shared.computer.terminal.NetworkedTerminal;
import dan200.computercraft.shared.computer.terminal.TerminalState;
import dan200.computercraft.shared.network.NetworkMessage;
import dan200.computercraft.shared.pocket.core.PocketServerComputer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

/**
 * Provides additional data about a client computer, such as its ID and current state.
 */
public class PocketComputerDataMessage implements NetworkMessage {
    private final int instanceId;
    private final ComputerState state;
    private final int lightState;
    private final TerminalState terminal;

    public PocketComputerDataMessage(PocketServerComputer computer, boolean sendTerminal) {
        instanceId = computer.getInstanceID();
        state = computer.getState();
        lightState = computer.getLight();
        terminal = sendTerminal ? computer.getTerminalState() : new TerminalState((NetworkedTerminal) null);
    }

    public PocketComputerDataMessage(FriendlyByteBuf buf) {
        instanceId = buf.readVarInt();
        state = buf.readEnum(ComputerState.class);
        lightState = buf.readVarInt();
        terminal = new TerminalState(buf);
    }

    @Override
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeVarInt(instanceId);
        buf.writeEnum(state);
        buf.writeVarInt(lightState);
        terminal.write(buf);
    }

    @Override
    public void handle(NetworkEvent.Context context) {
        var computer = ClientPocketComputers.get(instanceId, terminal.colour);
        computer.setState(state, lightState);
        if (terminal.hasTerminal()) computer.setTerminal(terminal);
    }
}
