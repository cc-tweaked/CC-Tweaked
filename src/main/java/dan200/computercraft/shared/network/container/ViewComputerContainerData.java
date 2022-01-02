/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.network.container;

import dan200.computercraft.core.terminal.Terminal;
import dan200.computercraft.shared.computer.core.ServerComputer;
import net.minecraft.network.FriendlyByteBuf;

import javax.annotation.Nonnull;

/**
 * View an arbitrary computer on the client.
 *
 * @see dan200.computercraft.shared.command.CommandComputerCraft
 */
public class ViewComputerContainerData extends ComputerContainerData
{
    private final int width;
    private final int height;

    public ViewComputerContainerData( ServerComputer computer )
    {
        super( computer );
        Terminal terminal = computer.getTerminal();
        if( terminal != null )
        {
            width = terminal.getWidth();
            height = terminal.getHeight();
        }
        else
        {
            width = height = 0;
        }
    }

    public ViewComputerContainerData( FriendlyByteBuf buffer )
    {
        super( buffer );
        width = buffer.readVarInt();
        height = buffer.readVarInt();
    }

    @Override
    public void toBytes( @Nonnull FriendlyByteBuf buf )
    {
        super.toBytes( buf );
        buf.writeVarInt( width );
        buf.writeVarInt( height );
    }

    public int getWidth()
    {
        return width;
    }

    public int getHeight()
    {
        return height;
    }
}
