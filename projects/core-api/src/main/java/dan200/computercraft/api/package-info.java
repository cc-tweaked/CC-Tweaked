// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

/**
 * ComputerCraft's public API.
 * <p>
 * You probably want to start in the following places:
 * <ul>
 *     <li>{@link dan200.computercraft.api.peripheral.IPeripheral} for registering new peripherals.</li>
 *     <li>
 *         {@link dan200.computercraft.api.lua.LuaFunction} and {@link dan200.computercraft.api.lua.IArguments} for
 *          adding methods to your peripheral or Lua objects.
 *     </li>
 * </ul>
 */
@DefaultQualifier(value = NonNull.class, locations = {
    TypeUseLocation.RETURN,
    TypeUseLocation.PARAMETER,
    TypeUseLocation.FIELD,
})
package dan200.computercraft.api;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;
import org.checkerframework.framework.qual.TypeUseLocation;
