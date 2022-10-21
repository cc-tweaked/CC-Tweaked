/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.network.server;

import dan200.computercraft.shared.computer.menu.ComputerMenu;
import dan200.computercraft.shared.computer.menu.ServerInputHandler;
import net.minecraft.inventory.container.Container;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;

import javax.annotation.Nonnull;

public class KeyEventServerMessage extends ComputerServerMessage
{
    public static final int TYPE_DOWN = 0;
    public static final int TYPE_REPEAT = 1;
    public static final int TYPE_UP = 2;

    private final int type;
    private final int key;

    public KeyEventServerMessage( Container menu, int type, int key )
    {
        super( menu );
        this.type = type;
        this.key = key;
    }

    public KeyEventServerMessage( @Nonnull PacketBuffer buf )
    {
        super( buf );
        type = buf.readByte();
        key = buf.readVarInt();
    }

    @Override
    public void toBytes( @Nonnull PacketBuffer buf )
    {
        buf.writeByte( type );
        buf.writeVarInt( key );
    }

    @Override
    protected void handle( NetworkEvent.Context context, @Nonnull ComputerMenu container )
    {
        ServerInputHandler input = container.getInput();
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
