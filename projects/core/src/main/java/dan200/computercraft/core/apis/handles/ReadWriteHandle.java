// SPDX-FileCopyrightText: 2018 The CC: Tweaked Developers
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
 * A file handle opened for reading and writing with {@link dan200.computercraft.core.apis.FSAPI#open(String, String)}.
 *
 * @cc.module fs.ReadWriteHandle
 */
public class ReadWriteHandle extends AbstractHandle {
    public ReadWriteHandle(SeekableByteChannel channel, TrackingCloseable closeable, boolean binary) {
        super(channel, closeable, binary);
    }

    /**
     * {@inheritDoc}
     */
    @Nullable
    @Override
    @LuaFunction
    public final Object[] read(Optional<Integer> countArg) throws LuaException {
        return super.read(countArg);
    }

    /**
     * {@inheritDoc}
     */
    @Nullable
    @Override
    @LuaFunction
    public final Object[] readAll() throws LuaException {
        return super.readAll();
    }

    /**
     * {@inheritDoc}
     */
    @Nullable
    @Override
    @LuaFunction
    public final Object[] readLine(Optional<Boolean> withTrailingArg) throws LuaException {
        return super.readLine(withTrailingArg);
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


}
