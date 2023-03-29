// Copyright Daniel Ratcliffe, 2011-2022. This API may be redistributed unmodified and in full only.
//
// SPDX-License-Identifier: LicenseRef-CCPL

package dan200.computercraft.api.lua;

import dan200.computercraft.api.peripheral.IDynamicPeripheral;

/**
 * An interface for representing custom objects returned by peripherals or other Lua objects.
 * <p>
 * Generally, one does not need to implement this type - it is sufficient to return an object with some methods
 * annotated with {@link LuaFunction}. {@link IDynamicLuaObject} is useful when you wish your available methods to
 * change at runtime.
 */
public interface IDynamicLuaObject {
    /**
     * Get the names of the methods that this object implements. This should not change over the course of the object's
     * lifetime.
     *
     * @return The method names this object provides.
     * @see IDynamicPeripheral#getMethodNames()
     */
    String[] getMethodNames();

    /**
     * Called when a user calls one of the methods that this object implements.
     *
     * @param context   The context of the currently running lua thread. This can be used to wait for events
     *                  or otherwise yield.
     * @param method    An integer identifying which method index from {@link #getMethodNames()} the computer wishes
     *                  to call.
     * @param arguments The arguments for this method.
     * @return The result of this function. Either an immediate value ({@link MethodResult#of(Object...)} or an
     * instruction to yield.
     * @throws LuaException If the function threw an exception.
     */
    MethodResult callMethod(ILuaContext context, int method, IArguments arguments) throws LuaException;
}
