/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.network.server;

import dan200.computercraft.shared.computer.core.IContainerComputer;
import dan200.computercraft.shared.computer.core.InputState;
import dan200.computercraft.shared.computer.core.ServerComputer;
import javax.annotation.Nonnull;
import net.minecraft.network.PacketByteBuf;

public class KeyEventServerMessage extends ComputerServerMessage
{
    public static final int TYPE_DOWN = 0;
    public static final int TYPE_REPEAT = 1;
    public static final int TYPE_UP = 2;

    private int type;
    private int key;

    public KeyEventServerMessage( int instanceId, int type, int key )
    {
        super( instanceId );
        this.type = type;
        this.key = key;
    }

    public KeyEventServerMessage()
    {
    }

    @Override
    public void toBytes( @Nonnull PacketByteBuf buf )
    {
        super.toBytes( buf );
        buf.writeByte( type );
        buf.writeVarInt( key );
    }

    @Override
    public void fromBytes( @Nonnull PacketByteBuf buf )
    {
        super.fromBytes( buf );
        type = buf.readByte();
        key = buf.readVarInt();
    }

    @Override
    protected void handle( @Nonnull ServerComputer computer, @Nonnull IContainerComputer container )
    {
        InputState input = container.getInput();
        if( type == TYPE_UP )
        {
            input.keyUp( key );
        }
        else
        {
            input.keyDown( key, type == TYPE_REPEAT );
        }
    }
}
