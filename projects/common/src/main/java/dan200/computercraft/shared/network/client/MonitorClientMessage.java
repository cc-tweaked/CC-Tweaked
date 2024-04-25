// SPDX-FileCopyrightText: 2020 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.network.client;

import dan200.computercraft.shared.computer.terminal.TerminalState;
import dan200.computercraft.shared.network.NetworkMessage;
import dan200.computercraft.shared.network.NetworkMessages;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.Optional;

/**
 * Update the terminal contents of a monitor.
 *
 * @param pos      The position of the origin monitor.
 * @param terminal The current monitor terminal.
 */
public record MonitorClientMessage(
    BlockPos pos, Optional<TerminalState> terminal
) implements NetworkMessage<ClientNetworkContext> {
    public static final StreamCodec<RegistryFriendlyByteBuf, MonitorClientMessage> STREAM_CODEC = StreamCodec.composite(
        BlockPos.STREAM_CODEC, MonitorClientMessage::pos,
        ByteBufCodecs.optional(TerminalState.STREAM_CODEC), MonitorClientMessage::terminal,
        MonitorClientMessage::new
    );

    @Override
    public void handle(ClientNetworkContext context) {
        context.handleMonitorData(pos, terminal.orElse(null));
    }

    @Override
    public CustomPacketPayload.Type<MonitorClientMessage> type() {
        return NetworkMessages.MONITOR_CLIENT;
    }
}
