/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.core.apis.handles;

import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.core.filesystem.TrackingCloseable;

import javax.annotation.Nonnull;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

/**
 * A file handle opened with {@link dan200.computercraft.core.apis.FSAPI#open(String, String)} with the {@code "r"}
 * mode.
 *
 * @cc.module fs.ReadHandle
 */
public class EncodedReadableHandle extends HandleGeneric
{
    private static final int BUFFER_SIZE = 8192;

    private final BufferedReader reader;

    public EncodedReadableHandle( @Nonnull BufferedReader reader, @Nonnull TrackingCloseable closable )
    {
        super( closable );
        this.reader = reader;
    }

    public EncodedReadableHandle( @Nonnull BufferedReader reader )
    {
        this( reader, new TrackingCloseable.Impl( reader ) );
    }

    /**
     * Read a line from the file.
     *
     * @param withTrailingArg Whether to include the newline characters with the returned string. Defaults to {@code false}.
     * @return The read string.
     * @throws LuaException If the file has been closed.
     * @cc.treturn string|nil The read line or {@code nil} if at the end of the file.
     * @cc.changed 1.81.0 Added option to return trailing newline.
     */
    @LuaFunction
    public final Object[] readLine( Optional<Boolean> withTrailingArg ) throws LuaException
    {
        checkOpen();
        boolean withTrailing = withTrailingArg.orElse( false );
        try
        {
            String line = reader.readLine();
            if( line != null )
            {
                // While this is technically inaccurate, it's better than nothing
                if( withTrailing ) line += "\n";
                return new Object[] { line };
            }
            else
            {
                return null;
            }
        }
        catch( IOException e )
        {
            return null;
        }
    }

    /**
     * Read the remainder of the file.
     *
     * @return The file, or {@code null} if at the end of it.
     * @throws LuaException If the file has been closed.
     * @cc.treturn nil|string The remaining contents of the file, or {@code nil} if we are at the end.
     */
    @LuaFunction
    public final Object[] readAll() throws LuaException
    {
        checkOpen();
        try
        {
            StringBuilder result = new StringBuilder();
            String line = reader.readLine();
            while( line != null )
            {
                result.append( line );
                line = reader.readLine();
                if( line != null )
                {
                    result.append( "\n" );
                }
            }
            return new Object[] { result.toString() };
        }
        catch( IOException e )
        {
            return null;
        }
    }

    /**
     * Read a number of characters from this file.
     *
     * @param countA The number of characters to read, defaulting to 1.
     * @return The read characters.
     * @throws LuaException When trying to read a negative number of characters.
     * @throws LuaException If the file has been closed.
     * @cc.treturn string|nil The read characters, or {@code nil} if at the of the file.
     * @cc.since 1.80pr1.4
     */
    @LuaFunction
    public final Object[] read( Optional<Integer> countA ) throws LuaException
    {
        checkOpen();
        try
        {
            int count = countA.orElse( 1 );
            if( count < 0 )
            {
                // Whilst this may seem absurd to allow reading 0 characters, PUC Lua it so
                // it seems best to remain somewhat consistent.
                throw new LuaException( "Cannot read a negative number of characters" );
            }
            else if( count <= BUFFER_SIZE )
            {
                // If we've got a small count, then allocate that and read it.
                char[] chars = new char[count];
                int read = reader.read( chars );

                return read < 0 ? null : new Object[] { new String( chars, 0, read ) };
            }
            else
            {
                // If we've got a large count, read in bunches of 8192.
                char[] buffer = new char[BUFFER_SIZE];

                // Read the initial set of characters, failing if none are read.
                int read = reader.read( buffer, 0, Math.min( buffer.length, count ) );
                if( read < 0 ) return null;

                StringBuilder out = new StringBuilder( read );
                int totalRead = read;
                out.append( buffer, 0, read );

                // Otherwise read until we either reach the limit or we no longer consume
                // the full buffer.
                while( read >= BUFFER_SIZE && totalRead < count )
                {
                    read = reader.read( buffer, 0, Math.min( BUFFER_SIZE, count - totalRead ) );
                    if( read < 0 ) break;

                    totalRead += read;
                    out.append( buffer, 0, read );
                }

                return new Object[] { out.toString() };
            }
        }
        catch( IOException e )
        {
            return null;
        }
    }

    public static BufferedReader openUtf8( ReadableByteChannel channel )
    {
        return open( channel, StandardCharsets.UTF_8 );
    }

    public static BufferedReader open( ReadableByteChannel channel, Charset charset )
    {
        // Create a charset decoder with the same properties as StreamDecoder does for
        // InputStreams: namely, replace everything instead of erroring.
        CharsetDecoder decoder = charset.newDecoder()
            .onMalformedInput( CodingErrorAction.REPLACE )
            .onUnmappableCharacter( CodingErrorAction.REPLACE );
        return new BufferedReader( Channels.newReader( channel, decoder, -1 ) );
    }
}
