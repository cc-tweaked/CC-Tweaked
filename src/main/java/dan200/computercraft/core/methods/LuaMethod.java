// SPDX-FileCopyrightText: 2020 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.core.methods;

import dan200.computer.core.ILuaObject;
import dan200.computercraft.api.lua.IArguments;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.lua.MethodResult;

/**
 * A basic Lua function (i.e. one not associated with a peripheral) on some object (such as a {@link ILuaObject}).
 * <p>
 * This interface is not typically implemented yourself, but instead generated from a {@link LuaFunction}-annotated
 * method.
 *
 * @see NamedMethod
 */
@FunctionalInterface
public interface LuaMethod {
    /**
     * Apply this method.
     *
     * @param target The object instance that this method targets.
     * @param args   Arguments to this function.
     * @return The return call of this function.
     * @throws LuaException Thrown by the underlying method call.
     */
    MethodResult apply(Object target, IArguments args) throws Exception;
}
