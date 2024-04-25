// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.network.client;

import dan200.computercraft.shared.computer.core.ComputerState;
import dan200.computercraft.shared.computer.core.ServerComputer;
import dan200.computercraft.shared.computer.terminal.TerminalState;
import dan200.computercraft.shared.network.NetworkMessage;
import dan200.computercraft.shared.network.NetworkMessages;
import dan200.computercraft.shared.network.codec.MoreStreamCodecs;
import dan200.computercraft.shared.pocket.core.PocketServerComputer;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.Optional;
import java.util.UUID;

/**
 * Provides additional data about a client computer, such as its ID and current state.
 *
 * @param id         The {@linkplain ServerComputer#getInstanceUUID() instance id} of the pocket computer.
 * @param state      Whether the computer is on, off, or blinking.
 * @param lightState The colour of the light, or {@code -1} if off.
 * @param terminal   The computer's terminal. This may be absent, in which case the terminal will not be updated on.
 */
public record PocketComputerDataMessage(
    UUID id, ComputerState state, int lightState, Optional<TerminalState> terminal
) implements NetworkMessage<ClientNetworkContext> {
    public static final StreamCodec<RegistryFriendlyByteBuf, PocketComputerDataMessage> STREAM_CODEC = StreamCodec.composite(
        UUIDUtil.STREAM_CODEC, PocketComputerDataMessage::id,
        MoreStreamCodecs.ofEnum(ComputerState.class), PocketComputerDataMessage::state,
        ByteBufCodecs.VAR_INT, PocketComputerDataMessage::lightState,
        ByteBufCodecs.optional(TerminalState.STREAM_CODEC), PocketComputerDataMessage::terminal,
        PocketComputerDataMessage::new
    );

    public PocketComputerDataMessage(PocketServerComputer computer, boolean sendTerminal) {
        this(
            computer.getInstanceUUID(),
            computer.getState(),
            computer.getLight(),
            sendTerminal ? Optional.of(computer.getTerminalState()) : Optional.empty()
        );
    }

    @Override
    public void handle(ClientNetworkContext context) {
        context.handlePocketComputerData(id, state, lightState, terminal.orElse(null));
    }

    @Override
    public CustomPacketPayload.Type<PocketComputerDataMessage> type() {
        return NetworkMessages.POCKET_COMPUTER_DATA;
    }
}
