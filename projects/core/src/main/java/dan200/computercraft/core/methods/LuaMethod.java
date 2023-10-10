// SPDX-FileCopyrightText: 2020 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.core.methods;

import dan200.computercraft.api.lua.*;

/**
 * A basic Lua function (i.e. one not associated with a peripheral) on some object (such as a {@link IDynamicLuaObject}
 * or {@link ILuaAPI}.
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
     * @param target  The object instance that this method targets.
     * @param context The Lua context for this function call.
     * @param args    Arguments to this function.
     * @return The return call of this function.
     * @throws LuaException Thrown by the underlying method call.
     * @see IDynamicLuaObject#callMethod(ILuaContext, int, IArguments)
     */
    MethodResult apply(Object target, ILuaContext context, IArguments args) throws LuaException;
}
