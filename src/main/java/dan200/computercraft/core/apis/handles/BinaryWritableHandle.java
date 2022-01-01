/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.core.apis.handles;

import dan200.computercraft.api.lua.IArguments;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.lua.LuaValues;
import dan200.computercraft.core.filesystem.TrackingCloseable;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.Optional;

/**
 * A file handle opened by {@link dan200.computercraft.core.apis.FSAPI#open} using the {@code "wb"} or {@code "ab"}
 * modes.
 *
 * @cc.module fs.BinaryWriteHandle
 */
public class BinaryWritableHandle extends HandleGeneric
{
    private final WritableByteChannel writer;
    final SeekableByteChannel seekable;
    private final ByteBuffer single = ByteBuffer.allocate( 1 );

    protected BinaryWritableHandle( WritableByteChannel writer, SeekableByteChannel seekable, TrackingCloseable closeable )
    {
        super( closeable );
        this.writer = writer;
        this.seekable = seekable;
    }

    public static BinaryWritableHandle of( WritableByteChannel channel, TrackingCloseable closeable )
    {
        SeekableByteChannel seekable = asSeekable( channel );
        return seekable == null ? new BinaryWritableHandle( channel, null, closeable ) : new Seekable( seekable, closeable );
    }

    public static BinaryWritableHandle of( WritableByteChannel channel )
    {
        return of( channel, new TrackingCloseable.Impl( channel ) );
    }

    /**
     * Write a string or byte to the file.
     *
     * @param arguments The value to write.
     * @throws LuaException If the file has been closed.
     * @cc.tparam [1] number The byte to write.
     * @cc.tparam [2] string The string to write.
     * @cc.changed 1.80pr1 Now accepts a string to write multiple bytes.
     */
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
            // Technically this is not needed
            if( writer instanceof FileChannel channel ) channel.force( false );
        }
        catch( IOException ignored )
        {
        }
    }

    public static class Seekable extends BinaryWritableHandle
    {
        public Seekable( SeekableByteChannel seekable, TrackingCloseable closeable )
        {
            super( seekable, seekable, closeable );
        }

        /**
         * Seek to a new position within the file, changing where bytes are written to. The new position is an offset
         * given by {@code offset}, relative to a start position determined by {@code whence}:
         *
         * - {@code "set"}: {@code offset} is relative to the beginning of the file.
         * - {@code "cur"}: Relative to the current position. This is the default.
         * - {@code "end"}: Relative to the end of the file.
         *
         * In case of success, {@code seek} returns the new file position from the beginning of the file.
         *
         * @param whence Where the offset is relative to.
         * @param offset The offset to seek to.
         * @return The new position.
         * @throws LuaException If the file has been closed.
         * @cc.treturn [1] number The new position.
         * @cc.treturn [2] nil If seeking failed.
         * @cc.treturn string The reason seeking failed.
         * @cc.since 1.80pr1.9
         */
        @LuaFunction
        public final Object[] seek( Optional<String> whence, Optional<Long> offset ) throws LuaException
        {
            checkOpen();
            return handleSeek( seekable, whence, offset );
        }
    }
}
