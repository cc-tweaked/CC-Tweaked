/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.core.apis.handles;

import dan200.computercraft.api.lua.ILuaContext;
import dan200.computercraft.api.lua.LuaException;

import javax.annotation.Nonnull;
import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.IOException;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;

public class EncodedWritableHandle extends HandleGeneric
{
    private BufferedWriter m_writer;

    public EncodedWritableHandle( @Nonnull BufferedWriter writer, @Nonnull Closeable closable )
    {
        super( closable );
        this.m_writer = writer;
    }

    public EncodedWritableHandle( @Nonnull BufferedWriter writer )
    {
        this( writer, writer );
    }

    @Nonnull
    @Override
    public String[] getMethodNames()
    {
        return new String[] {
            "write",
            "writeLine",
            "flush",
            "close",
        };
    }

    @Override
    public Object[] callMethod( @Nonnull ILuaContext context, int method, @Nonnull Object[] args ) throws LuaException
    {
        switch( method )
        {
            case 0: // write
            {
                checkOpen();
                String text = args.length > 0 && args[0] != null ? args[0].toString() : "";
                try
                {
                    m_writer.write( text, 0, text.length() );
                    return null;
                }
                catch( IOException e )
                {
                    throw new LuaException( e.getMessage() );
                }
            }
            case 1: // writeLine
            {
                checkOpen();
                String text = args.length > 0 && args[0] != null ? args[0].toString() : "";
                try
                {
                    m_writer.write( text, 0, text.length() );
                    m_writer.newLine();
                    return null;
                }
                catch( IOException e )
                {
                    throw new LuaException( e.getMessage() );
                }
            }
            case 2: // flush
                checkOpen();
                try
                {
                    m_writer.flush();
                    return null;
                }
                catch( IOException e )
                {
                    return null;
                }
            case 3: // close
                close();
                return null;
            default:
                return null;
        }
    }


    public static BufferedWriter openUtf8( WritableByteChannel channel )
    {
        return open( channel, StandardCharsets.UTF_8 );
    }

    public static BufferedWriter open( WritableByteChannel channel, Charset charset )
    {
        // Create a charset encoder with the same properties as StreamEncoder does for
        // OutputStreams: namely, replace everything instead of erroring.
        CharsetEncoder encoder = charset.newEncoder()
            .onMalformedInput( CodingErrorAction.REPLACE )
            .onUnmappableCharacter( CodingErrorAction.REPLACE );
        return new BufferedWriter( Channels.newWriter( channel, encoder, -1 ) );
    }
}
