/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.network.server;

import dan200.computercraft.shared.computer.core.IContainerComputer;
import dan200.computercraft.shared.computer.core.InputState;
import dan200.computercraft.shared.computer.core.ServerComputer;
import net.minecraft.network.PacketBuffer;

import javax.annotation.Nonnull;

public class MouseEventServerMessage extends ComputerServerMessage
{
    public static final int TYPE_CLICK = 0;
    public static final int TYPE_DRAG = 1;
    public static final int TYPE_UP = 2;
    public static final int TYPE_SCROLL = 3;
    public static final int TYPE_MOVE = 4;

    private int type;
    private int x;
    private int y;
    private int arg;

    public MouseEventServerMessage( int instanceId, int type, int arg, int x, int y )
    {
        super( instanceId );
        this.type = type;
        this.arg = arg;
        this.x = x;
        this.y = y;
    }

    public MouseEventServerMessage( int instanceId, int type, int x, int y )
    {
        this( instanceId, type, 0, x, y );
    }

    public MouseEventServerMessage()
    {
    }

    @Override
    public void toBytes( @Nonnull PacketBuffer buf )
    {
        super.toBytes( buf );
        buf.writeByte( type );
        buf.writeVarInt( arg );
        buf.writeVarInt( x );
        buf.writeVarInt( y );
    }

    @Override
    public void fromBytes( @Nonnull PacketBuffer buf )
    {
        super.fromBytes( buf );
        type = buf.readByte();
        arg = buf.readVarInt();
        x = buf.readVarInt();
        y = buf.readVarInt();
    }

    @Override
    protected void handle( @Nonnull ServerComputer computer, @Nonnull IContainerComputer container )
    {
        InputState input = container.getInput();
        switch( type )
        {
            case TYPE_CLICK:
                input.mouseClick( arg, x, y );
                break;
            case TYPE_DRAG:
                input.mouseDrag( arg, x, y );
                break;
            case TYPE_UP:
                input.mouseUp( arg, x, y );
                break;
            case TYPE_SCROLL:
                input.mouseScroll( arg, x, y );
                break;
            case TYPE_MOVE:
                input.mouseMove( x, y );
                break;
        }
    }
}
