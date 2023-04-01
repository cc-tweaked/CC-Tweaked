// SPDX-FileCopyrightText: 2020 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.api.lua;

/**
 * A function, which can be called from Lua. If you need to return a table of functions, it is recommended to use
 * an object with {@link LuaFunction} methods, or implement {@link IDynamicLuaObject}.
 *
 * @see MethodResult#of(Object)
 */
@FunctionalInterface
public interface ILuaFunction {
    /**
     * Call this function with a series of arguments. Note, this will <em>always</em> be called on the computer thread,
     * and so its implementation must be thread-safe.
     *
     * @param arguments The arguments for this function
     * @return The result of calling this function.
     * @throws LuaException Upon Lua errors.
     */
    MethodResult call(IArguments arguments) throws LuaException;
}
