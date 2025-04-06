// SPDX-FileCopyrightText: 2018 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.core.apis.handles;

import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaFunction;
import org.jspecify.annotations.Nullable;

import java.nio.channels.SeekableByteChannel;
import java.util.Optional;

/**
 * A file handle opened for reading with {@link dan200.computercraft.core.apis.FSAPI#open(String, String)}.
 *
 * @cc.module fs.ReadHandle
 */
public class ReadHandle extends AbstractHandle {
    public ReadHandle(SeekableByteChannel channel, boolean binary) {
        super(channel, binary);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @LuaFunction
    public final Object @Nullable [] read(Optional<Integer> countArg) throws LuaException {
        return super.read(countArg);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @LuaFunction
    public final Object @Nullable [] readAll() throws LuaException {
        return super.readAll();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @LuaFunction
    public final Object @Nullable [] readLine(Optional<Boolean> withTrailingArg) throws LuaException {
        return super.readLine(withTrailingArg);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @LuaFunction
    public final Object @Nullable [] seek(Optional<String> whence, Optional<Long> offset) throws LuaException {
        return super.seek(whence, offset);
    }
}
