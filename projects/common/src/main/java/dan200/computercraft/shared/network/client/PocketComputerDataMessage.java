// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.network.client;

import dan200.computercraft.shared.computer.core.ComputerState;
import dan200.computercraft.shared.computer.terminal.NetworkedTerminal;
import dan200.computercraft.shared.computer.terminal.TerminalState;
import dan200.computercraft.shared.network.MessageType;
import dan200.computercraft.shared.network.NetworkMessage;
import dan200.computercraft.shared.network.NetworkMessages;
import dan200.computercraft.shared.pocket.core.PocketServerComputer;
import net.minecraft.network.FriendlyByteBuf;

/**
 * Provides additional data about a client computer, such as its ID and current state.
 */
public class PocketComputerDataMessage implements NetworkMessage<ClientNetworkContext> {
    private final int instanceId;
    private final ComputerState state;
    private final int primaryLightState;
    private final int secondaryLightState;
    private final TerminalState terminal;

    public PocketComputerDataMessage(PocketServerComputer computer, boolean sendTerminal) {
        instanceId = computer.getInstanceID();
        state = computer.getState();
        primaryLightState = computer.getLightPrimary();
        secondaryLightState = computer.getLightSecondary();
        terminal = sendTerminal ? computer.getTerminalState() : new TerminalState((NetworkedTerminal) null);
    }

    public PocketComputerDataMessage(FriendlyByteBuf buf) {
        instanceId = buf.readVarInt();
        state = buf.readEnum(ComputerState.class);
        primaryLightState = buf.readVarInt();
        secondaryLightState = buf.readVarInt();
        terminal = new TerminalState(buf);
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeVarInt(instanceId);
        buf.writeEnum(state);
        buf.writeVarInt(primaryLightState);
        buf.writeVarInt(secondaryLightState);
        terminal.write(buf);
    }

    @Override
    public void handle(ClientNetworkContext context) {
        context.handlePocketComputerData(instanceId, state, primaryLightState, secondaryLightState, terminal);
    }

    @Override
    public MessageType<PocketComputerDataMessage> type() {
        return NetworkMessages.POCKET_COMPUTER_DATA;
    }
}
