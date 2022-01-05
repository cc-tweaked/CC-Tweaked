/*
 * This file is part of the public ComputerCraft API - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. This API may be redistributed unmodified and in full only.
 * For help using the API, and posting your mods, visit the forums at computercraft.info.
 */
package dan200.computercraft.api.lua;

import javax.annotation.Nonnull;

/**
 * A function, which can be called from Lua. If you need to return a table of functions, it is recommended to use
 * an object with {@link LuaFunction} methods, or implement {@link IDynamicLuaObject}.
 *
 * @see MethodResult#of(Object)
 */
@FunctionalInterface
public interface ILuaFunction
{
    /**
     * Call this function with a series of arguments. Note, this will <em>always</em> be called on the computer thread,
     * and so its implementation must be thread-safe.
     *
     * @param arguments The arguments for this function
     * @return The result of calling this function.
     * @throws LuaException Upon Lua errors.
     */
    @Nonnull
    MethodResult call( @Nonnull IArguments arguments ) throws LuaException;
}
