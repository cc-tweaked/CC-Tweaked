/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.core.apis.handles;

import dan200.computercraft.api.lua.IArguments;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.core.filesystem.TrackingCloseable;
import dan200.computercraft.shared.util.StringUtil;

import javax.annotation.Nonnull;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;

/**
 * A file handle opened by {@link dan200.computercraft.core.apis.FSAPI#open} using the {@code "w"} or {@code "a"} modes.
 *
 * @cc.module fs.WriteHandle
 */
public class EncodedWritableHandle extends HandleGeneric
{
    private final BufferedWriter writer;

    public EncodedWritableHandle( @Nonnull BufferedWriter writer, @Nonnull TrackingCloseable closable )
    {
        super( closable );
        this.writer = writer;
    }

    /**
     * Write a string of characters to the file.
     *
     * @param args The value to write.
     * @throws LuaException If the file has been closed.
     * @cc.param value The value to write to the file.
     */
    @LuaFunction
    public final void write( IArguments args ) throws LuaException
    {
        checkOpen();
        String text = StringUtil.toString( args.get( 0 ) );
        try
        {
            writer.write( text, 0, text.length() );
        }
        catch( IOException e )
        {
            throw new LuaException( e.getMessage() );
        }
    }

    /**
     * Write a string of characters to the file, follwing them with a new line character.
     *
     * @param args The value to write.
     * @throws LuaException If the file has been closed.
     * @cc.param value The value to write to the file.
     */
    @LuaFunction
    public final void writeLine( IArguments args ) throws LuaException
    {
        checkOpen();
        String text = StringUtil.toString( args.get( 0 ) );
        try
        {
            writer.write( text, 0, text.length() );
            writer.newLine();
        }
        catch( IOException e )
        {
            throw new LuaException( e.getMessage() );
        }
    }

    /**
     * Save the current file without closing it.
     *
     * @throws LuaException If the file has been closed.
     */
    @LuaFunction
    public final void flush() throws LuaException
    {
        checkOpen();
        try
        {
            writer.flush();
        }
        catch( IOException ignored )
        {
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
