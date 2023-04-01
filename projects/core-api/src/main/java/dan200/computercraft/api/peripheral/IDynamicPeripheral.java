// Copyright Daniel Ratcliffe, 2011-2022. This API may be redistributed unmodified and in full only.
//
// SPDX-License-Identifier: LicenseRef-CCPL

package dan200.computercraft.api.peripheral;

import dan200.computercraft.api.lua.*;

/**
 * A peripheral whose methods are not known at runtime.
 * <p>
 * This behaves similarly to {@link IDynamicLuaObject}, though also accepting the current {@link IComputerAccess}.
 * Generally one may use {@link LuaFunction} instead of implementing this interface.
 */
public interface IDynamicPeripheral extends IPeripheral {
    /**
     * Should return an array of strings that identify the methods that this peripheral exposes to Lua. This will be
     * called once before each attachment, and should not change when called multiple times.
     *
     * @return An array of strings representing method names.
     * @see #callMethod
     */
    String[] getMethodNames();

    /**
     * This is called when a lua program on an attached computer calls {@code peripheral.call()} with
     * one of the methods exposed by {@link #getMethodNames()}.
     * <p>
     * Be aware that this will be called from the ComputerCraft Lua thread, and must be thread-safe when interacting
     * with Minecraft objects.
     *
     * @param computer  The interface to the computer that is making the call. Remember that multiple
     *                  computers can be attached to a peripheral at once.
     * @param context   The context of the currently running lua thread. This can be used to wait for events
     *                  or otherwise yield.
     * @param method    An integer identifying which of the methods from getMethodNames() the computercraft
     *                  wishes to call. The integer indicates the index into the getMethodNames() table
     *                  that corresponds to the string passed into peripheral.call()
     * @param arguments The arguments for this method.
     * @return A {@link MethodResult} containing the values to return or the action to perform.
     * @throws LuaException If you throw any exception from this function, a lua error will be raised with the
     *                      same message as your exception. Use this to throw appropriate errors if the wrong
     *                      arguments are supplied to your method.
     * @see #getMethodNames()
     */
    MethodResult callMethod(IComputerAccess computer, ILuaContext context, int method, IArguments arguments) throws LuaException;
}
