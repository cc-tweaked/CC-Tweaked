// SPDX-FileCopyrightText: 2019 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.network.server;

import dan200.computercraft.shared.computer.menu.ComputerMenu;
import dan200.computercraft.shared.network.MessageType;
import dan200.computercraft.shared.network.NetworkMessages;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.inventory.AbstractContainerMenu;


public class MouseEventServerMessage extends ComputerServerMessage {
    private final Action type;
    private final int x;
    private final int y;
    private final int arg;

    public MouseEventServerMessage(AbstractContainerMenu menu, Action type, int arg, int x, int y) {
        super(menu);
        this.type = type;
        this.arg = arg;
        this.x = x;
        this.y = y;
    }

    public MouseEventServerMessage(FriendlyByteBuf buf) {
        super(buf);
        type = buf.readEnum(Action.class);
        arg = buf.readVarInt();
        x = buf.readVarInt();
        y = buf.readVarInt();
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        super.write(buf);
        buf.writeEnum(type);
        buf.writeVarInt(arg);
        buf.writeVarInt(x);
        buf.writeVarInt(y);
    }

    @Override
    protected void handle(ServerNetworkContext context, ComputerMenu container) {
        var input = container.getInput();
        switch (type) {
            case CLICK -> input.mouseClick(arg, x, y);
            case DRAG -> input.mouseDrag(arg, x, y);
            case UP -> input.mouseUp(arg, x, y);
            case SCROLL -> input.mouseScroll(arg, x, y);
        }
    }

    @Override
    public MessageType<MouseEventServerMessage> type() {
        return NetworkMessages.MOUSE_EVENT;
    }

    public enum Action {
        CLICK, DRAG, UP, SCROLL,
    }
}
