// SPDX-FileCopyrightText: 2017 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.core.apis.handles;

import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.core.filesystem.TrackingCloseable;
import dan200.computercraft.core.util.IoUtil;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.channels.SeekableByteChannel;
import java.util.Optional;

public abstract class HandleGeneric {
    private @Nullable TrackingCloseable closeable;

    protected HandleGeneric(TrackingCloseable closeable) {
        this.closeable = closeable;
    }

    protected void checkOpen() throws LuaException {
        var closeable = this.closeable;
        if (closeable == null || !closeable.isOpen()) throw new LuaException("attempt to use a closed file");
    }

    protected final void close() {
        IoUtil.closeQuietly(closeable);
        closeable = null;
    }

    /**
     * Close this file, freeing any resources it uses.
     * <p>
     * Once a file is closed it may no longer be read or written to.
     *
     * @throws LuaException If the file has already been closed.
     */
    @LuaFunction("close")
    public final void doClose() throws LuaException {
        checkOpen();
        close();
    }


    /**
     * Shared implementation for various file handle types.
     *
     * @param channel The channel to seek in
     * @param whence  The seeking mode.
     * @param offset  The offset to seek to.
     * @return The new position of the file, or null if some error occurred.
     * @throws LuaException If the arguments were invalid
     * @see <a href="https://www.lua.org/manual/5.1/manual.html#pdf-file:seek">{@code file:seek} in the Lua manual.</a>
     */
    @Nullable
    protected static Object[] handleSeek(SeekableByteChannel channel, Optional<String> whence, Optional<Long> offset) throws LuaException {
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
}
