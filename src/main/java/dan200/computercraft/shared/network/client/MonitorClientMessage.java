/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.network.client;

import dan200.computercraft.shared.computer.terminal.TerminalState;
import dan200.computercraft.shared.network.NetworkMessage;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;


public class MonitorClientMessage implements NetworkMessage<ClientNetworkContext> {
    private final BlockPos pos;
    private final TerminalState state;

    public MonitorClientMessage(BlockPos pos, TerminalState state) {
        this.pos = pos;
        this.state = state;
    }

    public MonitorClientMessage(FriendlyByteBuf buf) {
        pos = buf.readBlockPos();
        state = new TerminalState(buf);
    }

    @Override
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        state.write(buf);
    }

    @Override
    public void handle(ClientNetworkContext context) {
        context.handleMonitorData(pos, state);
    }
}
