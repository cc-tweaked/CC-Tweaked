// SPDX-FileCopyrightText: 2018 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.network.server;

import dan200.computercraft.shared.computer.menu.ComputerMenu;
import dan200.computercraft.shared.network.MessageType;
import dan200.computercraft.shared.network.NetworkMessages;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.inventory.AbstractContainerMenu;


public class ComputerActionServerMessage extends ComputerServerMessage {
    private final Action action;

    public ComputerActionServerMessage(AbstractContainerMenu menu, Action action) {
        super(menu);
        this.action = action;
    }

    public ComputerActionServerMessage(FriendlyByteBuf buf) {
        super(buf);
        action = buf.readEnum(Action.class);
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        super.write(buf);
        buf.writeEnum(action);
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
    public MessageType<ComputerActionServerMessage> type() {
        return NetworkMessages.COMPUTER_ACTION;
    }

    public enum Action {
        TURN_ON,
        SHUTDOWN,
        REBOOT
    }
}
