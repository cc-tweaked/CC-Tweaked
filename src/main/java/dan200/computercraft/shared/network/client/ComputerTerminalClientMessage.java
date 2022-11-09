/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.network.client;

import dan200.computercraft.shared.computer.terminal.TerminalState;
import dan200.computercraft.shared.network.NetworkMessage;
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
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeVarInt(containerId);
        terminal.write(buf);
    }

    @Override
    public void handle(ClientNetworkContext context) {
        context.handleComputerTerminal(containerId, terminal);
    }
}
