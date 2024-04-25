// SPDX-FileCopyrightText: 2020 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.network.client;

import dan200.computercraft.shared.computer.terminal.TerminalState;
import dan200.computercraft.shared.network.MessageType;
import dan200.computercraft.shared.network.NetworkMessage;
import dan200.computercraft.shared.network.NetworkMessages;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;

import javax.annotation.Nullable;

public class MonitorClientMessage implements NetworkMessage<ClientNetworkContext> {
    private final BlockPos pos;
    private final @Nullable TerminalState state;

    public MonitorClientMessage(BlockPos pos, @Nullable TerminalState state) {
        this.pos = pos;
        this.state = state;
    }

    public MonitorClientMessage(FriendlyByteBuf buf) {
        pos = buf.readBlockPos();
        state = buf.readNullable(TerminalState::new);
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeNullable(state, (b, t) -> t.write(b));
    }

    @Override
    public void handle(ClientNetworkContext context) {
        context.handleMonitorData(pos, state);
    }

    @Override
    public MessageType<MonitorClientMessage> type() {
        return NetworkMessages.MONITOR_CLIENT;
    }
}
