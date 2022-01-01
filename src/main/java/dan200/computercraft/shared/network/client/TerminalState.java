/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.network.client;

import dan200.computercraft.core.terminal.Terminal;
import dan200.computercraft.shared.util.IoUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

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

    private final boolean compress;

    @Nullable
    private final ByteBuf buffer;

    private ByteBuf compressed;

    public TerminalState( boolean colour, @Nullable Terminal terminal )
    {
        this( colour, terminal, true );
    }

    public TerminalState( boolean colour, @Nullable Terminal terminal, boolean compress )
    {
        this.colour = colour;
        this.compress = compress;

        if( terminal == null )
        {
            width = height = 0;
            buffer = null;
        }
        else
        {
            width = terminal.getWidth();
            height = terminal.getHeight();

            ByteBuf buf = buffer = Unpooled.buffer();
            terminal.write( new FriendlyByteBuf( buf ) );
        }
    }

    public TerminalState( FriendlyByteBuf buf )
    {
        colour = buf.readBoolean();
        compress = buf.readBoolean();

        if( buf.readBoolean() )
        {
            width = buf.readVarInt();
            height = buf.readVarInt();

            int length = buf.readVarInt();
            buffer = readCompressed( buf, length, compress );
        }
        else
        {
            width = height = 0;
            buffer = null;
        }
    }

    public void write( FriendlyByteBuf buf )
    {
        buf.writeBoolean( colour );
        buf.writeBoolean( compress );

        buf.writeBoolean( buffer != null );
        if( buffer != null )
        {
            buf.writeVarInt( width );
            buf.writeVarInt( height );

            ByteBuf sendBuffer = getCompressed();
            buf.writeVarInt( sendBuffer.readableBytes() );
            buf.writeBytes( sendBuffer, sendBuffer.readerIndex(), sendBuffer.readableBytes() );
        }
    }

    public boolean hasTerminal()
    {
        return buffer != null;
    }

    public int size()
    {
        return buffer == null ? 0 : buffer.readableBytes();
    }

    public void apply( Terminal terminal )
    {
        if( buffer == null ) throw new NullPointerException( "buffer" );
        terminal.read( new FriendlyByteBuf( buffer ) );
    }

    private ByteBuf getCompressed()
    {
        if( buffer == null ) throw new NullPointerException( "buffer" );
        if( !compress ) return buffer;
        if( compressed != null ) return compressed;

        ByteBuf compressed = Unpooled.buffer();
        OutputStream stream = null;
        try
        {
            stream = new GZIPOutputStream( new ByteBufOutputStream( compressed ) );
            stream.write( buffer.array(), buffer.arrayOffset(), buffer.readableBytes() );
        }
        catch( IOException e )
        {
            throw new UncheckedIOException( e );
        }
        finally
        {
            IoUtil.closeQuietly( stream );
        }

        return this.compressed = compressed;
    }

    private static ByteBuf readCompressed( ByteBuf buf, int length, boolean compress )
    {
        if( compress )
        {
            ByteBuf buffer = Unpooled.buffer();
            InputStream stream = null;
            try
            {
                stream = new GZIPInputStream( new ByteBufInputStream( buf, length ) );
                byte[] swap = new byte[8192];
                while( true )
                {
                    int bytes = stream.read( swap );
                    if( bytes == -1 ) break;
                    buffer.writeBytes( swap, 0, bytes );
                }
            }
            catch( IOException e )
            {
                throw new UncheckedIOException( e );
            }
            finally
            {
                IoUtil.closeQuietly( stream );
            }
            return buffer;
        }
        else
        {
            ByteBuf buffer = Unpooled.buffer( length );
            buf.readBytes( buffer, length );
            return buffer;
        }
    }
}
