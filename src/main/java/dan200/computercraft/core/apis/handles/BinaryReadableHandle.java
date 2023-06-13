// SPDX-FileCopyrightText: 2018 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.core.apis.handles;

import dan200.computer.core.IMountedFileBinary;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.core.apis.FSAPI;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Optional;

/**
 * A file handle opened with {@link FSAPI#open(String, String)} with the {@code "rb"}
 * mode.
 *
 * @cc.module fs.BinaryReadHandle
 */
public class BinaryReadableHandle extends HandleGeneric {
    private final IMountedFileBinary channel;

    public BinaryReadableHandle(IMountedFileBinary channel) {
        super(channel);
        this.channel = channel;
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
    @LuaFunction
    public final Object[] read(Optional<Integer> countArg) throws LuaException {
        checkOpen();
        try {
            if (countArg.isPresent()) {
                int count = countArg.get();
                if (count < 0) throw new LuaException("Cannot read a negative number of bytes");

                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                boolean readAnything = false;
                for (int i = 0; i < count; i++) {
                    int r = channel.read();
                    if (r == -1) break;

                    readAnything = true;
                    stream.write(r);
                }

                return readAnything ? new Object[]{ stream.toByteArray() } : null;
            } else {
                int b = channel.read();
                return b == -1 ? null : new Object[]{ b };
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
    @LuaFunction
    public final Object[] readAll() throws LuaException {
        checkOpen();
        try {
            int expected = 32;
            ByteArrayOutputStream stream = new ByteArrayOutputStream(expected);

            boolean readAnything = false;
            while (true) {
                int r = channel.read();
                if (r == -1) break;

                readAnything = true;
                stream.write(r);
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
    @LuaFunction
    public final Object[] readLine(Optional<Boolean> withTrailingArg) throws LuaException {
        checkOpen();
        boolean withTrailing = withTrailingArg.orElse(false);
        try {
            ByteArrayOutputStream stream = new ByteArrayOutputStream();

            boolean readAnything = false, readRc = false;
            while (true) {
                int read = channel.read();
                if (read < 0) {
                    // Nothing else to read, and we saw no \n. Return the array. If we saw a \r, then add it
                    // back.
                    if (readRc) stream.write('\r');
                    return readAnything ? new Object[]{ stream.toByteArray() } : null;
                }

                readAnything = true;

                if (read == '\n') {
                    if (withTrailing) {
                        if (readRc) stream.write('\r');
                        stream.write(read);
                    }
                    return new Object[]{ stream.toByteArray() };
                } else {
                    // We want to skip \r\n, but obviously need to include cases where \r is not followed by \n.
                    // Note, this behaviour is non-standard compliant (strictly speaking we should have no
                    // special logic for \r), but we preserve compatibility with EncodedReadableHandle and
                    // previous behaviour of the io library.
                    if (readRc) stream.write('\r');
                    readRc = read == '\r';
                    if (!readRc) stream.write(read);
                }
            }
        } catch (IOException e) {
            return null;
        }
    }
}
