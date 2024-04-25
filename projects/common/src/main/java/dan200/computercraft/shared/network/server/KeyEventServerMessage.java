// SPDX-FileCopyrightText: 2019 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.network.server;

import dan200.computercraft.shared.computer.menu.ComputerMenu;
import dan200.computercraft.shared.network.MessageType;
import dan200.computercraft.shared.network.NetworkMessages;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.inventory.AbstractContainerMenu;


public class KeyEventServerMessage extends ComputerServerMessage {
    private final Action type;
    private final int key;

    public KeyEventServerMessage(AbstractContainerMenu menu, Action type, int key) {
        super(menu);
        this.type = type;
        this.key = key;
    }

    public KeyEventServerMessage(FriendlyByteBuf buf) {
        super(buf);
        type = buf.readEnum(Action.class);
        key = buf.readVarInt();
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        super.write(buf);
        buf.writeEnum(type);
        buf.writeVarInt(key);
    }

    @Override
    protected void handle(ServerNetworkContext context, ComputerMenu container) {
        var input = container.getInput();
        if (type == Action.UP) {
            input.keyUp(key);
        } else {
            input.keyDown(key, type == Action.REPEAT);
        }
    }

    @Override
    public MessageType<KeyEventServerMessage> type() {
        return NetworkMessages.KEY_EVENT;
    }

    public enum Action {
        DOWN, REPEAT, UP
    }
}
