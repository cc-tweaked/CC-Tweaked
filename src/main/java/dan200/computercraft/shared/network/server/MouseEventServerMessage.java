/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.network.server;

import dan200.computercraft.shared.computer.core.IContainerComputer;
import dan200.computercraft.shared.computer.core.InputState;
import dan200.computercraft.shared.computer.core.ServerComputer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import javax.annotation.Nonnull;

public class MouseEventServerMessage extends ComputerServerMessage
{
    public static final int TYPE_CLICK = 0;
    public static final int TYPE_DRAG = 1;
    public static final int TYPE_UP = 2;
    public static final int TYPE_SCROLL = 3;

    private final int type;
    private final int x;
    private final int y;
    private final int arg;

    public MouseEventServerMessage( int instanceId, int type, int arg, int x, int y )
    {
        super( instanceId );
        this.type = type;
        this.arg = arg;
        this.x = x;
        this.y = y;
    }

    public MouseEventServerMessage( @Nonnull FriendlyByteBuf buf )
    {
        super( buf );
        type = buf.readByte();
        arg = buf.readVarInt();
        x = buf.readVarInt();
        y = buf.readVarInt();
    }

    @Override
    public void toBytes( @Nonnull FriendlyByteBuf buf )
    {
        super.toBytes( buf );
        buf.writeByte( type );
        buf.writeVarInt( arg );
        buf.writeVarInt( x );
        buf.writeVarInt( y );
    }

    @Override
    protected void handle( NetworkEvent.Context context, @Nonnull ServerComputer computer, @Nonnull IContainerComputer container )
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
        }
    }
}
