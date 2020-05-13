/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.core.apis.handles;

import dan200.computercraft.api.lua.IArguments;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaFunction;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SeekableByteChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class BinaryReadableHandle extends HandleGeneric
{
    private static final int BUFFER_SIZE = 8192;

    private final ReadableByteChannel reader;
    final SeekableByteChannel seekable;
    private final ByteBuffer single = ByteBuffer.allocate( 1 );

    protected BinaryReadableHandle( ReadableByteChannel reader, SeekableByteChannel seekable, Closeable closeable )
    {
        super( closeable );
        this.reader = reader;
        this.seekable = seekable;
    }

    public static BinaryReadableHandle of( ReadableByteChannel channel, Closeable closeable )
    {
        SeekableByteChannel seekable = asSeekable( channel );
        return seekable == null ? new BinaryReadableHandle( channel, null, closeable ) : new Seekable( seekable, closeable );
    }

    public static BinaryReadableHandle of( ReadableByteChannel channel )
    {
        return of( channel, channel );
    }

    @LuaFunction
    public final Object[] read( Optional<Integer> countArg ) throws LuaException
    {
        checkOpen();
        try
        {
            if( countArg.isPresent() )
            {
                int count = countArg.get();
                if( count < 0 ) throw new LuaException( "Cannot read a negative number of bytes" );
                if( count == 0 && seekable != null )
                {
                    return seekable.position() >= seekable.size() ? null : new Object[] { "" };
                }

                if( count <= BUFFER_SIZE )
                {
                    ByteBuffer buffer = ByteBuffer.allocate( count );

                    int read = reader.read( buffer );
                    if( read < 0 ) return null;
                    buffer.flip();
                    return new Object[] { buffer };
                }
                else
                {
                    // Read the initial set of characters, failing if none are read.
                    ByteBuffer buffer = ByteBuffer.allocate( BUFFER_SIZE );
                    int read = reader.read( buffer );
                    if( read < 0 ) return null;

                    // If we failed to read "enough" here, let's just abort
                    if( read >= count || read < BUFFER_SIZE )
                    {
                        buffer.flip();
                        return new Object[] { buffer };
                    }

                    // Build up an array of ByteBuffers. Hopefully this means we can perform less allocation
                    // than doubling up the buffer each time.
                    int totalRead = read;
                    List<ByteBuffer> parts = new ArrayList<>( 4 );
                    parts.add( buffer );
                    while( read >= BUFFER_SIZE && totalRead < count )
                    {
                        buffer = ByteBuffer.allocate( Math.min( BUFFER_SIZE, count - totalRead ) );
                        read = reader.read( buffer );
                        if( read < 0 ) break;

                        totalRead += read;
                        parts.add( buffer );
                    }

                    // Now just copy all the bytes across!
                    byte[] bytes = new byte[totalRead];
                    int pos = 0;
                    for( ByteBuffer part : parts )
                    {
                        System.arraycopy( part.array(), 0, bytes, pos, part.position() );
                        pos += part.position();
                    }
                    return new Object[] { bytes };
                }
            }
            else
            {
                single.clear();
                int b = reader.read( single );
                return b == -1 ? null : new Object[] { single.get( 0 ) & 0xFF };
            }
        }
        catch( IOException e )
        {
            return null;
        }
    }

    @LuaFunction
    public final Object[] readAll() throws LuaException
    {
        checkOpen();
        try
        {
            int expected = 32;
            if( seekable != null ) expected = Math.max( expected, (int) (seekable.size() - seekable.position()) );
            ByteArrayOutputStream stream = new ByteArrayOutputStream( expected );

            ByteBuffer buf = ByteBuffer.allocate( 8192 );
            boolean readAnything = false;
            while( true )
            {
                buf.clear();
                int r = reader.read( buf );
                if( r == -1 ) break;

                readAnything = true;
                stream.write( buf.array(), 0, r );
            }
            return readAnything ? new Object[] { stream.toByteArray() } : null;
        }
        catch( IOException e )
        {
            return null;
        }
    }

    @LuaFunction
    public final Object[] readLine( Optional<Boolean> withTrailingArg ) throws LuaException
    {
        checkOpen();
        boolean withTrailing = withTrailingArg.orElse( false );
        try
        {
            ByteArrayOutputStream stream = new ByteArrayOutputStream();

            boolean readAnything = false, readRc = false;
            while( true )
            {
                single.clear();
                int read = reader.read( single );
                if( read <= 0 )
                {
                    // Nothing else to read, and we saw no \n. Return the array. If we saw a \r, then add it
                    // back.
                    if( readRc ) stream.write( '\r' );
                    return readAnything ? new Object[] { stream.toByteArray() } : null;
                }

                readAnything = true;

                byte chr = single.get( 0 );
                if( chr == '\n' )
                {
                    if( withTrailing )
                    {
                        if( readRc ) stream.write( '\r' );
                        stream.write( chr );
                    }
                    return new Object[] { stream.toByteArray() };
                }
                else
                {
                    // We want to skip \r\n, but obviously need to include cases where \r is not followed by \n.
                    // Note, this behaviour is non-standard compliant (strictly speaking we should have no
                    // special logic for \r), but we preserve compatibility with EncodedReadableHandle and
                    // previous behaviour of the io library.
                    if( readRc ) stream.write( '\r' );
                    readRc = chr == '\r';
                    if( !readRc ) stream.write( chr );
                }
            }
        }
        catch( IOException e )
        {
            return null;
        }
    }

    public static class Seekable extends BinaryReadableHandle
    {
        public Seekable( SeekableByteChannel seekable, Closeable closeable )
        {
            super( seekable, seekable, closeable );
        }

        @LuaFunction
        public final Object[] seek( IArguments arguments ) throws LuaException
        {
            checkOpen();
            return handleSeek( seekable, arguments );
        }
    }
}
