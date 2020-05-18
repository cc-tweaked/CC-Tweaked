/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.network.client;

import dan200.computercraft.core.terminal.Terminal;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.network.PacketBuffer;

import javax.annotation.Nullable;
import java.util.Objects;

/**
 * A snapshot of a terminal's state.
 *
 * This is somewhat memory inefficient (we build a buffer, only to write it elsewhere), however it means we get a
 * complete and accurate description of a terminal, which avoids a lot of complexities with resizing terminals, dirty
 * states, etc...
 */
public class TerminalState
{
    public final boolean colour;

    public final int width;
    public final int height;

    @Nullable
    private final ByteBuf buffer;

    public TerminalState( boolean colour, @Nullable Terminal terminal )
    {
        this.colour = colour;

        if( terminal == null )
        {
            this.width = this.height = 0;
            this.buffer = null;
        }
        else
        {
            this.width = terminal.getWidth();
            this.height = terminal.getHeight();

            ByteBuf buf = Unpooled.directBuffer();
            terminal.write( new PacketBuffer( buf ) );
            this.buffer = buf;
        }
    }

    public TerminalState( PacketBuffer buf )
    {
        this.colour = buf.readBoolean();
        if( buf.readBoolean() )
        {
            this.width = buf.readVarInt();
            this.height = buf.readVarInt();

            int length = buf.readVarInt();
            ByteBuf terminal = this.buffer = Unpooled.directBuffer( length );
            buf.readBytes( terminal );
        }
        else
        {
            this.width = this.height = 0;
            this.buffer = null;
        }
    }

    public void write( PacketBuffer buf )
    {
        buf.writeBoolean( colour );
        buf.writeBoolean( buffer != null );
        if( buffer != null )
        {
            buf.writeVarInt( width );
            buf.writeVarInt( height );

            buf.writeVarInt( buffer.readableBytes() );
            int index = buffer.readerIndex();
            buf.writeBytes( buffer );
            buffer.readerIndex( index );
        }
    }

    public boolean hasTerminal()
    {
        return buffer != null;
    }

    public void apply( Terminal terminal )
    {
        terminal.read( new PacketBuffer( Objects.requireNonNull( buffer ) ) );
    }
}
