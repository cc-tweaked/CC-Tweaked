// SPDX-FileCopyrightText: 2017 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.core.apis.handles;

import dan200.computer.core.IMountedFile;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaFunction;

import java.io.IOException;

public abstract class HandleGeneric {
    private IMountedFile closeable;

    protected HandleGeneric(IMountedFile closeable) {
        this.closeable = closeable;
    }

    protected void checkOpen() throws LuaException {
        if (closeable == null) throw new LuaException("attempt to use a closed file");
    }

    protected final void close() {
        try {
            closeable.close();
        } catch (IOException ignored) {
        }
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
}
