/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.network.server;

import javax.annotation.Nonnull;

import dan200.computercraft.shared.computer.core.IContainerComputer;
import dan200.computercraft.shared.computer.core.InputState;
import dan200.computercraft.shared.computer.core.ServerComputer;

import net.minecraft.network.PacketByteBuf;

public class MouseEventServerMessage extends ComputerServerMessage {
    public static final int TYPE_CLICK = 0;
    public static final int TYPE_DRAG = 1;
    public static final int TYPE_UP = 2;
    public static final int TYPE_SCROLL = 3;

    private int type;
    private int x;
    private int y;
    private int arg;

    public MouseEventServerMessage(int instanceId, int type, int arg, int x, int y) {
        super(instanceId);
        this.type = type;
        this.arg = arg;
        this.x = x;
        this.y = y;
    }

    public MouseEventServerMessage() {
    }

    @Override
    public void toBytes(@Nonnull PacketByteBuf buf) {
        super.toBytes(buf);
        buf.writeByte(this.type);
        buf.writeVarInt(this.arg);
        buf.writeVarInt(this.x);
        buf.writeVarInt(this.y);
    }

    @Override
    public void fromBytes(@Nonnull PacketByteBuf buf) {
        super.fromBytes(buf);
        this.type = buf.readByte();
        this.arg = buf.readVarInt();
        this.x = buf.readVarInt();
        this.y = buf.readVarInt();
    }

    @Override
    protected void handle(@Nonnull ServerComputer computer, @Nonnull IContainerComputer container) {
        InputState input = container.getInput();
        switch (this.type) {
        case TYPE_CLICK:
            input.mouseClick(this.arg, this.x, this.y);
            break;
        case TYPE_DRAG:
            input.mouseDrag(this.arg, this.x, this.y);
            break;
        case TYPE_UP:
            input.mouseUp(this.arg, this.x, this.y);
            break;
        case TYPE_SCROLL:
            input.mouseScroll(this.arg, this.x, this.y);
            break;
        }
    }
}
