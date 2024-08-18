// SPDX-FileCopyrightText: 2017 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.core.apis.handles;

import dan200.computercraft.api.lua.Coerced;
import dan200.computercraft.api.lua.IArguments;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.core.filesystem.TrackingCloseable;
import dan200.computercraft.core.util.IoUtil;

import javax.annotation.Nullable;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SeekableByteChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * The base class for all file handle types.
 */
public abstract class AbstractHandle {
    private static final int BUFFER_SIZE = 8192;

    private final SeekableByteChannel channel;
    private @Nullable TrackingCloseable closeable;
    protected final boolean binary;

    private final ByteBuffer single = ByteBuffer.allocate(1);

    protected AbstractHandle(SeekableByteChannel channel, TrackingCloseable closeable, boolean binary) {
        this.channel = channel;
        this.closeable = closeable;
        this.binary = binary;
    }

    protected void checkOpen() throws LuaException {
        var closeable = this.closeable;
        if (closeable == null || !closeable.isOpen()) throw new LuaException("attempt to use a closed file");
    }

    /**
     * Close this file, freeing any resources it uses.
     * <p>
     * Once a file is closed it may no longer be read or written to.
     *
     * @throws LuaException If the file has already been closed.
     */
    @LuaFunction
    public final void close() throws LuaException {
        checkOpen();
        IoUtil.closeQuietly(closeable);
        closeable = null;
    }

    /**
     * Seek to a new position within the file, changing where bytes are written to. The new position is an offset
     * given by {@code offset}, relative to a start position determined by {@code whence}:
     * <p>
     * - {@code "set"}: {@code offset} is relative to the beginning of the file.
     * - {@code "cur"}: Relative to the current position. This is the default.
     * - {@code "end"}: Relative to the end of the file.
     * <p>
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
    @Nullable
    public Object[] seek(Optional<String> whence, Optional<Long> offset) throws LuaException {
        checkOpen();
        long actualOffset = offset.orElse(0L);
        try {
            switch (whence.orElse("cur")) {
                case "set" -> channel.position(actualOffset);
                case "cur" -> channel.position(channel.position() + actualOffset);
                case "end" -> channel.position(channel.size() + actualOffset);
                default -> throw new LuaException("bad argument #1 to 'seek' (invalid option '" + whence + "'");
            }

            return new Object[]{ channel.position() };
        } catch (IllegalArgumentException e) {
            return new Object[]{ null, "Position is negative" };
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * Read a number of bytes from this file.
     *
     * @param countArg The number of bytes to read. This may be 0 to determine we are at the end of the file. When
     *                 absent, a single byte will be read.
     * @return The read bytes.
     * @throws LuaException When trying to read a negative number of bytes.
     * @throws LuaException If the file has been closed.
     * @cc.treturn [1] nil If we are at the end of the file.
     * @cc.treturn [2] number The value of the byte read. This is returned if the file is opened in binary mode and
     * {@code count} is absent
     * @cc.treturn [3] string The bytes read as a string. This is returned when the {@code count} is given.
     * @cc.changed 1.80pr1 Now accepts an integer argument to read multiple bytes, returning a string instead of a number.
     */
    @Nullable
    public Object[] read(Optional<Integer> countArg) throws LuaException {
        checkOpen();
        try {
            if (binary && countArg.isEmpty()) {
                single.clear();
                var b = channel.read(single);
                return b == -1 ? null : new Object[]{ single.get(0) & 0xFF };
            } else {
                int count = countArg.orElse(1);
                if (count < 0) throw new LuaException("Cannot read a negative number of bytes");
                if (count == 0) return channel.position() >= channel.size() ? null : new Object[]{ "" };

                if (count <= BUFFER_SIZE) {
                    var buffer = ByteBuffer.allocate(count);

                    var read = channel.read(buffer);
                    if (read < 0) return null;
                    buffer.flip();
                    return new Object[]{ buffer };
                } else {
                    // Read the initial set of characters, failing if none are read.
                    var buffer = ByteBuffer.allocate(BUFFER_SIZE);
                    var read = channel.read(buffer);
                    if (read < 0) return null;
                    buffer.flip();

                    // If we failed to read "enough" here, let's just abort
                    if (read >= count || read < BUFFER_SIZE) return new Object[]{ buffer };

                    // Build up an array of ByteBuffers. Hopefully this means we can perform less allocation
                    // than doubling up the buffer each time.
                    var totalRead = read;
                    List<ByteBuffer> parts = new ArrayList<>(4);
                    parts.add(buffer);
                    while (read >= BUFFER_SIZE && totalRead < count) {
                        buffer = ByteBuffer.allocateDirect(Math.min(BUFFER_SIZE, count - totalRead));
                        read = channel.read(buffer);
                        if (read < 0) break;
                        buffer.flip();

                        totalRead += read;
                        assert read == buffer.remaining();
                        parts.add(buffer);
                    }

                    // Now just copy all the bytes across!
                    var bytes = new byte[totalRead];
                    var pos = 0;
                    for (var part : parts) {
                        int length = part.remaining();
                        part.get(bytes, pos, length);
                        pos += length;
                    }
                    assert pos == totalRead;
                    return new Object[]{ bytes };
                }
            }
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * Read the remainder of the file.
     *
     * @return The remaining contents of the file, or {@code null} in the event of an error.
     * @throws LuaException If the file has been closed.
     * @cc.treturn string|nil The remaining contents of the file, or {@code nil} in the event of an error.
     * @cc.since 1.80pr1
     */
    @Nullable
    public Object[] readAll() throws LuaException {
        checkOpen();
        try {
            var expected = 32;
            expected = Math.max(expected, (int) (channel.size() - channel.position()));
            var stream = new ByteArrayOutputStream(expected);

            var buf = ByteBuffer.allocate(8192);
            while (true) {
                buf.clear();
                var r = channel.read(buf);
                if (r == -1) break;

                stream.write(buf.array(), 0, r);
            }
            return new Object[]{ stream.toByteArray() };
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * Read a line from the file.
     *
     * @param withTrailingArg Whether to include the newline characters with the returned string. Defaults to {@code false}.
     * @return The read string.
     * @throws LuaException If the file has been closed.
     * @cc.treturn string|nil The read line or {@code nil} if at the end of the file.
     * @cc.since 1.80pr1.9
     * @cc.changed 1.81.0 `\r` is now stripped.
     */
    @Nullable
    public Object[] readLine(Optional<Boolean> withTrailingArg) throws LuaException {
        checkOpen();
        boolean withTrailing = withTrailingArg.orElse(false);
        try {
            var stream = new ByteArrayOutputStream();

            boolean readAnything = false, readRc = false;
            while (true) {
                single.clear();
                var read = channel.read(single);
                if (read <= 0) {
                    // Nothing else to read, and we saw no \n. Return the array. If we saw a \r, then add it
                    // back.
                    if (readRc) stream.write('\r');
                    return readAnything ? new Object[]{ stream.toByteArray() } : null;
                }

                readAnything = true;

                var chr = single.get(0);
                if (chr == '\n') {
                    if (withTrailing) {
                        if (readRc) stream.write('\r');
                        stream.write(chr);
                    }
                    return new Object[]{ stream.toByteArray() };
                } else {
                    // We want to skip \r\n, but obviously need to include cases where \r is not followed by \n.
                    // Note, this behaviour is non-standard compliant (strictly speaking we should have no
                    // special logic for \r), but we preserve compatibility with EncodedReadableHandle and
                    // previous behaviour of the io library.
                    if (readRc) stream.write('\r');
                    readRc = chr == '\r';
                    if (!readRc) stream.write(chr);
                }
            }
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * Write a string or byte to the file.
     *
     * @param arguments The value to write.
     * @throws LuaException If the file has been closed.
     * @cc.tparam [1] string contents The string to write.
     * @cc.tparam [2] number charcode The byte to write, if the file was opened in binary mode.
     * @cc.changed 1.80pr1 Now accepts a string to write multiple bytes.
     */
    public void write(IArguments arguments) throws LuaException {
        checkOpen();
        try {
            var arg = arguments.get(0);
            if (binary && arg instanceof Number) {
                var number = ((Number) arg).intValue();
                writeSingle((byte) number);
            } else {
                channel.write(arguments.getBytesCoerced(0));
            }
        } catch (IOException e) {
            throw new LuaException(e.getMessage());
        }
    }

    /**
     * Write a string of characters to the file, following them with a new line character.
     *
     * @param text The text to write to the file.
     * @throws LuaException If the file has been closed.
     */
    public void writeLine(Coerced<ByteBuffer> text) throws LuaException {
        checkOpen();
        try {
            channel.write(text.value());
            writeSingle((byte) '\n');
        } catch (IOException e) {
            throw new LuaException(e.getMessage());
        }
    }

    private void writeSingle(byte value) throws IOException {
        single.clear();
        single.put(value);
        single.flip();
        channel.write(single);
    }

    /**
     * Save the current file without closing it.
     *
     * @throws LuaException If the file has been closed.
     */
    public void flush() throws LuaException {
        checkOpen();
        try {
            // Technically this is not needed
            if (channel instanceof FileChannel channel) channel.force(false);
        } catch (IOException e) {
            throw new LuaException(e.getMessage());
        }
    }
}
