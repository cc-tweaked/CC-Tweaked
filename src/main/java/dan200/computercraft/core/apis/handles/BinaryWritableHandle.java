// SPDX-FileCopyrightText: 2017 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.core.apis.handles;

import dan200.computer.core.IMountedFileBinary;
import dan200.computercraft.api.lua.ArgumentHelper;
import dan200.computercraft.api.lua.IArguments;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.core.apis.FSAPI;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * A file handle opened by {@link FSAPI#open} using the {@code "wb"} or {@code "ab"}
 * modes.
 *
 * @cc.module fs.BinaryWriteHandle
 */
public class BinaryWritableHandle extends HandleGeneric {
    private final IMountedFileBinary channel;

    public BinaryWritableHandle(IMountedFileBinary channel) {
        super(channel);
        this.channel = channel;
    }

    /**
     * Write a string or byte to the file.
     *
     * @param arguments The value to write.
     * @throws LuaException If the file has been closed.
     * @cc.tparam [1] number charcode The byte to write.
     * @cc.tparam [2] string contents The string to write.
     * @cc.changed 1.80pr1 Now accepts a string to write multiple bytes.
     */
    @LuaFunction
    public final void write(IArguments arguments) throws LuaException {
        checkOpen();
        try {
            Object arg = arguments.get(0);
            if (arg instanceof Number) {
                int number = ((Number) arg).intValue();
                channel.write(number);
            } else if (arg instanceof String) {
                ByteBuffer contents = arguments.getBytes(0);
                for (int i = contents.position(), length = contents.capacity(); i < length; i++) {
                    channel.write(contents.get(i));
                }
            } else {
                throw ArgumentHelper.badArgumentOf(0, "string or number", arg);
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
        checkOpen();
        try {
            channel.flush();
        } catch (IOException e) {
            throw new LuaException(e.getMessage());
        }
    }
}
