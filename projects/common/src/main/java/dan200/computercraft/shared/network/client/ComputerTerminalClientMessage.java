// SPDX-FileCopyrightText: 2018 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.network.client;

import dan200.computercraft.shared.computer.menu.ComputerMenu;
import dan200.computercraft.shared.computer.terminal.TerminalState;
import dan200.computercraft.shared.network.NetworkMessage;
import dan200.computercraft.shared.network.NetworkMessages;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.inventory.AbstractContainerMenu;

/**
 * Update the terminal for the currently opened {@link ComputerMenu}.
 *
 * @param containerId The currently opened container id.
 * @param terminal    The new terminal data.
 */
public record ComputerTerminalClientMessage(
    int containerId, TerminalState terminal
) implements NetworkMessage<ClientNetworkContext> {
    public static final StreamCodec<RegistryFriendlyByteBuf, ComputerTerminalClientMessage> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT, ComputerTerminalClientMessage::containerId,
        TerminalState.STREAM_CODEC, ComputerTerminalClientMessage::terminal,
        ComputerTerminalClientMessage::new
    );

    public ComputerTerminalClientMessage(AbstractContainerMenu menu, TerminalState terminal) {
        this(menu.containerId, terminal);
    }

    @Override
    public void handle(ClientNetworkContext context) {
        context.handleComputerTerminal(containerId, terminal);
    }

    @Override
    public CustomPacketPayload.Type<ComputerTerminalClientMessage> type() {
        return NetworkMessages.COMPUTER_TERMINAL;
    }
}
