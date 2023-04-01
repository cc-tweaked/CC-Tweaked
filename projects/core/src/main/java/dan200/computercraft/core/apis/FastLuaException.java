// SPDX-FileCopyrightText: 2020 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.core.apis;

import dan200.computercraft.api.lua.LuaException;

import javax.annotation.Nullable;
import java.io.Serial;

/**
 * A Lua exception which does not contain its stack trace.
 */
public class FastLuaException extends LuaException {
    @Serial
    private static final long serialVersionUID = 5957864899303561143L;

    public FastLuaException(@Nullable String message) {
        super(message);
    }

    public FastLuaException(@Nullable String message, int level) {
        super(message, level);
    }

    @Override
    public synchronized Throwable fillInStackTrace() {
        return this;
    }
}
