// Copyright Daniel Ratcliffe, 2011-2022. This API may be redistributed unmodified and in full only.
//
// SPDX-License-Identifier: LicenseRef-CCPL

package dan200.computercraft.api.lua;

import javax.annotation.Nullable;
import java.io.Serial;

/**
 * An exception representing an error in Lua, like that raised by the {@code error()} function.
 */
public class LuaException extends Exception {
    @Serial
    private static final long serialVersionUID = -6136063076818512651L;
    private final boolean hasLevel;
    private final int level;

    public LuaException(@Nullable String message) {
        super(message);
        hasLevel = false;
        level = 1;
    }

    public LuaException(@Nullable String message, int level) {
        super(message);
        hasLevel = true;
        this.level = level;
    }

    /**
     * Whether a level was explicitly specified when constructing. If a level is not provided, the Lua runtime may
     * attempt to pick the most suitable one.
     *
     * @return Whether this has an explicit level.
     */
    public boolean hasLevel() {
        return hasLevel;
    }

    /**
     * The level this error is raised at. Level 1 is the function's caller, level 2 is that function's caller, and so
     * on.
     *
     * @return The level to raise the error at.
     */
    public int getLevel() {
        return level;
    }
}
