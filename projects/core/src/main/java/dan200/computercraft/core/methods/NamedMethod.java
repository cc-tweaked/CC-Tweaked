// SPDX-FileCopyrightText: 2020 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.core.methods;

import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.lua.MethodResult;
import dan200.computercraft.api.peripheral.GenericPeripheral;
import dan200.computercraft.api.peripheral.PeripheralType;

import javax.annotation.Nullable;

/**
 * A method generated from a {@link LuaFunction}.
 *
 * @param name        The name of this method.
 * @param method      The underlying method implementation.
 * @param nonYielding If this method is guaranteed to never yield, and will always return a
 *                    {@linkplain MethodResult#of(Object...) basic result}.
 * @param genericType The peripheral type of this method. This is only set if this is a method on a
 *                    {@link GenericPeripheral}.
 * @param <T>         The type of method, either a {@link LuaMethod} or {@link PeripheralMethod}.
 */
public record NamedMethod<T>(String name, T method, boolean nonYielding, @Nullable PeripheralType genericType) {
}
