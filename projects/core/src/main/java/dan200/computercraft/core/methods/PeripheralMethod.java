// SPDX-FileCopyrightText: 2020 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.core.methods;

import dan200.computercraft.api.lua.*;
import dan200.computercraft.api.peripheral.IComputerAccess;
import dan200.computercraft.api.peripheral.IDynamicPeripheral;
import dan200.computercraft.api.peripheral.IPeripheral;

/**
 * A Lua function associated with some peripheral.
 * <p>
 * This interface is not typically implemented yourself, but instead generated from a {@link LuaFunction}-annotated
 * method.
 *
 * @see NamedMethod
 * @see IPeripheral
 */
@FunctionalInterface
public interface PeripheralMethod {
    /**
     * Apply this method.
     *
     * @param target   The object instance that this method targets.
     * @param context  The Lua context for this function call.
     * @param computer The interface to the computer that is making the call.
     * @param args     Arguments to this function.
     * @return The return call of this function.
     * @throws LuaException Thrown by the underlying method call.
     * @see IDynamicPeripheral#callMethod(IComputerAccess, ILuaContext, int, IArguments)
     */
    MethodResult apply(Object target, ILuaContext context, IComputerAccess computer, IArguments args) throws LuaException;
}
