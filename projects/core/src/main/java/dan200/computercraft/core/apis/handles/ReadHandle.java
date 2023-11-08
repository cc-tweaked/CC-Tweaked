// SPDX-FileCopyrightText: 2018 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.core.apis.handles;

import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.core.filesystem.TrackingCloseable;

import javax.annotation.Nullable;
import java.nio.channels.SeekableByteChannel;
import java.util.Optional;

/**
 * A file handle opened for reading with {@link dan200.computercraft.core.apis.FSAPI#open(String, String)}.
 *
 * @cc.module fs.ReadHandle
 */
public class ReadHandle extends AbstractHandle {
    public ReadHandle(SeekableByteChannel channel, TrackingCloseable closeable, boolean binary) {
        super(channel, closeable, binary);
    }

    public ReadHandle(SeekableByteChannel channel, boolean binary) {
        this(channel, new TrackingCloseable.Impl(channel), binary);
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
}
