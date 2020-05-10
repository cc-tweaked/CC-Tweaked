/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.core.apis.handles;

import dan200.computercraft.api.lua.IArguments;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.lua.LuaValues;

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.channels.WritableByteChannel;

public class BinaryWritableHandle extends HandleGeneric
{
    private final WritableByteChannel writer;
    final SeekableByteChannel seekable;
    private final ByteBuffer single = ByteBuffer.allocate( 1 );

    protected BinaryWritableHandle( WritableByteChannel writer, SeekableByteChannel seekable, Closeable closeable )
    {
        super( closeable );
        this.writer = writer;
        this.seekable = seekable;
    }

    public static BinaryWritableHandle of( WritableByteChannel channel, Closeable closeable )
    {
        SeekableByteChannel seekable = asSeekable( channel );
        return seekable == null ? new BinaryWritableHandle( channel, null, closeable ) : new Seekable( seekable, closeable );
    }

    public static BinaryWritableHandle of( WritableByteChannel channel )
    {
        return of( channel, channel );
    }

    @LuaFunction
    public final void write( IArguments arguments ) throws LuaException
    {
        checkOpen();
        try
        {
            Object arg = arguments.get( 0 );
            if( arg instanceof Number )
            {
                int number = ((Number) arg).intValue();
                single.clear();
                single.put( (byte) number );
                single.flip();

                writer.write( single );
            }
            else if( arg instanceof String )
            {
                writer.write( arguments.getBytes( 0 ) );
            }
            else
            {
                throw LuaValues.badArgumentOf( 0, "string or number", arg );
            }
        }
        catch( IOException e )
        {
            throw new LuaException( e.getMessage() );
        }
    }

    @LuaFunction
    public final void flush() throws LuaException
    {
        checkOpen();
        try
        {
            // Technically this is not needed
            if( writer instanceof FileChannel ) ((FileChannel) writer).force( false );
        }
        catch( IOException ignored )
        {
        }
    }

    public static class Seekable extends BinaryWritableHandle
    {
        public Seekable( SeekableByteChannel seekable, Closeable closeable )
        {
            super( seekable, seekable, closeable );
        }

        @LuaFunction
        public final Object seek( IArguments args ) throws LuaException
        {
            checkOpen();
            return handleSeek( seekable, args );
        }
    }
}
