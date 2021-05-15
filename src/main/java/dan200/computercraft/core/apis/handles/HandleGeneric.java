/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.core.apis.handles;

import java.io.Closeable;
import java.io.IOException;
import java.nio.channels.Channel;
import java.nio.channels.SeekableByteChannel;
import java.util.Optional;

import javax.annotation.Nonnull;

import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.shared.util.IoUtil;

public abstract class HandleGeneric {
    private Closeable closable;
    private boolean open = true;

    protected HandleGeneric(@Nonnull Closeable closable) {
        this.closable = closable;
    }

    /**
     * Shared implementation for various file handle types.
     *
     * @param channel The channel to seek in
     * @param whence The seeking mode.
     * @param offset The offset to seek to.
     * @return The new position of the file, or null if some error occurred.
     * @throws LuaException If the arguments were invalid
     * @see <a href="https://www.lua.org/manual/5.1/manual.html#pdf-file:seek">{@code file:seek} in the Lua manual.</a>
     */
    protected static Object[] handleSeek(SeekableByteChannel channel, Optional<String> whence, Optional<Long> offset) throws LuaException {
        long actualOffset = offset.orElse(0L);
        try {
            switch (whence.orElse("cur")) {
            case "set":
                channel.position(actualOffset);
                break;
            case "cur":
                channel.position(channel.position() + actualOffset);
                break;
            case "end":
                channel.position(channel.size() + actualOffset);
                break;
            default:
                throw new LuaException("bad argument #1 to 'seek' (invalid option '" + whence + "'");
            }

            return new Object[] {channel.position()};
        } catch (IllegalArgumentException e) {
            return new Object[] {
                null,
                "Position is negative"
            };
        } catch (IOException e) {
            return null;
        }
    }

    protected static SeekableByteChannel asSeekable(Channel channel) {
        if (!(channel instanceof SeekableByteChannel)) {
            return null;
        }

        SeekableByteChannel seekable = (SeekableByteChannel) channel;
        try {
            seekable.position(seekable.position());
            return seekable;
        } catch (IOException | UnsupportedOperationException e) {
            return null;
        }
    }

    /**
     * Close this file, freeing any resources it uses.
     *
     * Once a file is closed it may no longer be read or written to.
     *
     * @throws LuaException If the file has already been closed.
     */
    @LuaFunction ("close")
    public final void doClose() throws LuaException {
        this.checkOpen();
        this.close();
    }

    protected void checkOpen() throws LuaException {
        if (!this.open) {
            throw new LuaException("attempt to use a closed file");
        }
    }

    protected final void close() {
        this.open = false;

        IoUtil.closeQuietly(this.closable);
        this.closable = null;
    }
}
