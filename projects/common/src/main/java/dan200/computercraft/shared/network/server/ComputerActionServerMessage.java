// SPDX-FileCopyrightText: 2018 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.network.server;

import dan200.computercraft.shared.computer.menu.ComputerMenu;
import dan200.computercraft.shared.network.NetworkMessages;
import dan200.computercraft.shared.network.codec.MoreStreamCodecs;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.inventory.AbstractContainerMenu;

/**
 * Turn on, shutdown, or reboot the currently open computer.
 */
public final class ComputerActionServerMessage extends ComputerServerMessage {
    public static final StreamCodec<RegistryFriendlyByteBuf, ComputerActionServerMessage> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT, ComputerActionServerMessage::containerId,
        MoreStreamCodecs.ofEnum(Action.class), x -> x.action,
        ComputerActionServerMessage::new
    );

    private final Action action;

    public ComputerActionServerMessage(AbstractContainerMenu menu, Action action) {
        this(menu.containerId, action);
    }

    private ComputerActionServerMessage(int menu, Action action) {
        super(menu);
        this.action = action;
    }

    @Override
    protected void handle(ServerNetworkContext context, ComputerMenu container) {
        switch (action) {
            case TURN_ON -> container.getInput().turnOn();
            case REBOOT -> container.getInput().reboot();
            case SHUTDOWN -> container.getInput().shutdown();
        }
    }

    @Override
    public CustomPacketPayload.Type<ComputerActionServerMessage> type() {
        return NetworkMessages.COMPUTER_ACTION;
    }

    public enum Action {
        TURN_ON,
        SHUTDOWN,
        REBOOT
    }
}
