/*
 * This file is part of the public ComputerCraft API - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2018. This API may be redistributed unmodified and in full only.
 * For help using the API, and posting your mods, visit the forums at computercraft.info.
 */

package dan200.computercraft.api.lua;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * A Lua function which consumes some values and returns a result.
 *
 * @see MethodResult#then(ILuaFunction)
 * @see MethodResult#pullEvent(ILuaFunction)
 * @see MethodResult#pullEventRaw(String)
 */
@FunctionalInterface
public interface ILuaFunction
{
    /**
     * Accept the values and return another method result.
     *
     * @param values The inputs for this function.
     * @return The result of executing this function.
     * @throws LuaException If you throw any exception from this function, a lua error will be raised with the
     *                      same message as your exception. Use this to throw appropriate errors if the wrong
     *                      arguments are supplied to your method.
     */
    @Nonnull
    MethodResult call( @Nullable Object[] values ) throws LuaException;
}
