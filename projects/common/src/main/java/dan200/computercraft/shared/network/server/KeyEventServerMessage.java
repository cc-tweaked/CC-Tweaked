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
    public static final int TYPE_DOWN = 0;
    public static final int TYPE_REPEAT = 1;
    public static final int TYPE_UP = 2;

    private final int type;
    private final int key;

    public KeyEventServerMessage(AbstractContainerMenu menu, int type, int key) {
        super(menu);
        this.type = type;
        this.key = key;
    }

    public KeyEventServerMessage(FriendlyByteBuf buf) {
        super(buf);
        type = buf.readByte();
        key = buf.readVarInt();
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        super.write(buf);
        buf.writeByte(type);
        buf.writeVarInt(key);
    }

    @Override
    protected void handle(ServerNetworkContext context, ComputerMenu container) {
        var input = container.getInput();
        if (type == TYPE_UP) {
            input.keyUp(key);
        } else {
            input.keyDown(key, type == TYPE_REPEAT);
        }
    }

    @Override
    public MessageType<KeyEventServerMessage> type() {
        return NetworkMessages.KEY_EVENT;
    }
}
