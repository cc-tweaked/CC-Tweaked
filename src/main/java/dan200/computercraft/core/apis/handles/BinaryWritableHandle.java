/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.core.apis.handles;

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.Optional;

import dan200.computercraft.api.lua.IArguments;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.lua.LuaValues;

/**
 * A file handle opened by {@link dan200.computercraft.core.apis.FSAPI#open} using the {@code "wb"} or {@code "ab"} modes.
 *
 * @cc.module fs.BinaryWriteHandle
 */
public class BinaryWritableHandle extends HandleGeneric {
    final SeekableByteChannel seekable;
    private final WritableByteChannel writer;
    private final ByteBuffer single = ByteBuffer.allocate(1);

    protected BinaryWritableHandle(WritableByteChannel writer, SeekableByteChannel seekable, Closeable closeable) {
        super(closeable);
        this.writer = writer;
        this.seekable = seekable;
    }

    public static BinaryWritableHandle of(WritableByteChannel channel) {
        return of(channel, channel);
    }

    public static BinaryWritableHandle of(WritableByteChannel channel, Closeable closeable) {
        SeekableByteChannel seekable = asSeekable(channel);
        return seekable == null ? new BinaryWritableHandle(channel, null, closeable) : new Seekable(seekable, closeable);
    }

    /**
     * Write a string or byte to the file.
     *
     * @param arguments The value to write.
     * @throws LuaException If the file has been closed.
     * @cc.tparam [1] number The byte to write.
     * @cc.tparam [2] string The string to write.
     */
    @LuaFunction
    public final void write(IArguments arguments) throws LuaException {
        this.checkOpen();
        try {
            Object arg = arguments.get(0);
            if (arg instanceof Number) {
                int number = ((Number) arg).intValue();
                this.single.clear();
                this.single.put((byte) number);
                this.single.flip();

                this.writer.write(this.single);
            } else if (arg instanceof String) {
                this.writer.write(arguments.getBytes(0));
            } else {
                throw LuaValues.badArgumentOf(0, "string or number", arg);
            }
        } catch (IOException e) {
            throw new LuaException(e.getMessage());
        }
    }

    /**
     * Save the current file without closing it.
     *
     * @throws LuaException If the file has been closed.
     */
    @LuaFunction
    public final void flush() throws LuaException {
        this.checkOpen();
        try {
            // Technically this is not needed
            if (this.writer instanceof FileChannel) {
                ((FileChannel) this.writer).force(false);
            }
        } catch (IOException ignored) {
        }
    }

    public static class Seekable extends BinaryWritableHandle {
        public Seekable(SeekableByteChannel seekable, Closeable closeable) {
            super(seekable, seekable, closeable);
        }

        /**
         * Seek to a new position within the file, changing where bytes are written to. The new position is an offset given by {@code offset}, relative to a
         * start position determined by {@code whence}:
         *
         * - {@code "set"}: {@code offset} is relative to the beginning of the file. - {@code "cur"}: Relative to the current position. This is the default.
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
         */
        @LuaFunction
        public final Object[] seek(Optional<String> whence, Optional<Long> offset) throws LuaException {
            this.checkOpen();
            return handleSeek(this.seekable, whence, offset);
        }
    }
}
