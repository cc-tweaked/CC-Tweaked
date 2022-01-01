/*
 * This file is part of the public ComputerCraft API - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. This API may be redistributed unmodified and in full only.
 * For help using the API, and posting your mods, visit the forums at computercraft.info.
 */
package dan200.computercraft.api.lua;

import javax.annotation.Nonnull;

/**
 * A continuation which is called when this coroutine is resumed.
 *
 * @see MethodResult#yield(Object[], ILuaCallback)
 */
public interface ILuaCallback
{
    /**
     * Resume this coroutine.
     *
     * @param args The result of resuming this coroutine. These will have the same form as described in
     *             {@link LuaFunction}.
     * @return The result of this continuation. Either the result to return to the callee, or another yield.
     * @throws LuaException On an error.
     */
    @Nonnull
    MethodResult resume( Object[] args ) throws LuaException;
}
