/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.core.apis.handles;

import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.core.filesystem.TrackingCloseable;

import javax.annotation.Nullable;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * A file handle opened with {@link dan200.computercraft.core.apis.FSAPI#open(String, String)} with the {@code "rb"}
 * mode.
 *
 * @cc.module fs.BinaryReadHandle
 */
public class BinaryReadableHandle extends HandleGeneric {
    private static final int BUFFER_SIZE = 8192;

    private final SeekableByteChannel channel;
    private final ByteBuffer single = ByteBuffer.allocate(1);

    BinaryReadableHandle(SeekableByteChannel channel, TrackingCloseable closeable) {
        super(closeable);
        this.channel = channel;
    }

    public static BinaryReadableHandle of(SeekableByteChannel channel, TrackingCloseable closeable) {
        return new BinaryReadableHandle(channel, closeable);
    }

    public static BinaryReadableHandle of(SeekableByteChannel channel) {
        return of(channel, new TrackingCloseable.Impl(channel));
    }

    /**
     * Read a number of bytes from this file.
     *
     * @param countArg The number of bytes to read. When absent, a single byte will be read <em>as a number</em>. This
     *                 may be 0 to determine we are at the end of the file.
     * @return The read bytes.
     * @throws LuaException When trying to read a negative number of bytes.
     * @throws LuaException If the file has been closed.
     * @cc.treturn [1] nil If we are at the end of the file.
     * @cc.treturn [2] number The value of the byte read. This is returned when the {@code count} is absent.
     * @cc.treturn [3] string The bytes read as a string. This is returned when the {@code count} is given.
     * @cc.changed 1.80pr1 Now accepts an integer argument to read multiple bytes, returning a string instead of a number.
     */
    @Nullable
    @LuaFunction
    public final Object[] read(Optional<Integer> countArg) throws LuaException {
        checkOpen();
        try {
            if (countArg.isPresent()) {
                int count = countArg.get();
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

                    // If we failed to read "enough" here, let's just abort
                    if (read >= count || read < BUFFER_SIZE) {
                        buffer.flip();
                        return new Object[]{ buffer };
                    }

                    // Build up an array of ByteBuffers. Hopefully this means we can perform less allocation
                    // than doubling up the buffer each time.
                    var totalRead = read;
                    List<ByteBuffer> parts = new ArrayList<>(4);
                    parts.add(buffer);
                    while (read >= BUFFER_SIZE && totalRead < count) {
                        buffer = ByteBuffer.allocate(Math.min(BUFFER_SIZE, count - totalRead));
                        read = channel.read(buffer);
                        if (read < 0) break;

                        totalRead += read;
                        parts.add(buffer);
                    }

                    // Now just copy all the bytes across!
                    var bytes = new byte[totalRead];
                    var pos = 0;
                    for (var part : parts) {
                        System.arraycopy(part.array(), 0, bytes, pos, part.position());
                        pos += part.position();
                    }
                    return new Object[]{ bytes };
                }
            } else {
                single.clear();
                var b = channel.read(single);
                return b == -1 ? null : new Object[]{ single.get(0) & 0xFF };
            }
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * Read the remainder of the file.
     *
     * @return The file, or {@code null} if at the end of it.
     * @throws LuaException If the file has been closed.
     * @cc.treturn string|nil The remaining contents of the file, or {@code nil} if we are at the end.
     * @cc.since 1.80pr1
     */
    @Nullable
    @LuaFunction
    public final Object[] readAll() throws LuaException {
        checkOpen();
        try {
            var expected = 32;
            expected = Math.max(expected, (int) (channel.size() - channel.position()));
            var stream = new ByteArrayOutputStream(expected);

            var buf = ByteBuffer.allocate(8192);
            var readAnything = false;
            while (true) {
                buf.clear();
                var r = channel.read(buf);
                if (r == -1) break;

                readAnything = true;
                stream.write(buf.array(), 0, r);
            }
            return readAnything ? new Object[]{ stream.toByteArray() } : null;
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
    @LuaFunction
    public final Object[] readLine(Optional<Boolean> withTrailingArg) throws LuaException {
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
    @LuaFunction
    public final Object[] seek(Optional<String> whence, Optional<Long> offset) throws LuaException {
        checkOpen();
        return handleSeek(channel, whence, offset);
    }
}
