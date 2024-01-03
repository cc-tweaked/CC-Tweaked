// SPDX-FileCopyrightText: 2018 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.network.client;

import dan200.computercraft.shared.computer.terminal.TerminalState;
import dan200.computercraft.shared.network.MessageType;
import dan200.computercraft.shared.network.NetworkMessage;
import dan200.computercraft.shared.network.NetworkMessages;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.inventory.AbstractContainerMenu;


public class ComputerTerminalClientMessage implements NetworkMessage<ClientNetworkContext> {
    private final int containerId;
    private final TerminalState terminal;

    public ComputerTerminalClientMessage(AbstractContainerMenu menu, TerminalState terminal) {
        containerId = menu.containerId;
        this.terminal = terminal;
    }

    public ComputerTerminalClientMessage(FriendlyByteBuf buf) {
        containerId = buf.readVarInt();
        terminal = new TerminalState(buf);
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeVarInt(containerId);
        terminal.write(buf);
    }

    @Override
    public void handle(ClientNetworkContext context) {
        context.handleComputerTerminal(containerId, terminal);
    }

    @Override
    public MessageType<ComputerTerminalClientMessage> type() {
        return NetworkMessages.COMPUTER_TERMINAL;
    }
}
