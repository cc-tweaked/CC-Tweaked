// SPDX-FileCopyrightText: 2017 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.core.apis.handles;

import dan200.computercraft.api.lua.Coerced;
import dan200.computercraft.api.lua.IArguments;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.core.filesystem.TrackingCloseable;

import javax.annotation.Nullable;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.util.Optional;

/**
 * A file handle opened for writing by {@link dan200.computercraft.core.apis.FSAPI#open}.
 *
 * @cc.module fs.WriteHandle
 */
public class WriteHandle extends AbstractHandle {
    protected WriteHandle(SeekableByteChannel channel, TrackingCloseable closeable, boolean binary) {
        super(channel, closeable, binary);
    }

    public static WriteHandle of(SeekableByteChannel channel, TrackingCloseable closeable, boolean binary, boolean canSeek) {
        return canSeek ? new Seekable(channel, closeable, binary) : new WriteHandle(channel, closeable, binary);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @LuaFunction
    public final void write(IArguments arguments) throws LuaException {
        super.write(arguments);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @LuaFunction
    public final void writeLine(Coerced<ByteBuffer> text) throws LuaException {
        super.writeLine(text);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @LuaFunction
    public final void flush() throws LuaException {
        super.flush();
    }

    public static class Seekable extends WriteHandle {
        Seekable(SeekableByteChannel channel, TrackingCloseable closeable, boolean binary) {
            super(channel, closeable, binary);
        }

        /**
         * {@inheritDoc}
         */
        @Nullable
        @Override
        @LuaFunction
        public final Object[] seek(Optional<String> whence, Optional<Long> offset) throws LuaException {
            return super.seek(whence, offset);
        }
    }
}
