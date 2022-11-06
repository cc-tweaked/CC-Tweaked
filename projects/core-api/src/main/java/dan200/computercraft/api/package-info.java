/*
 * This file is part of the public ComputerCraft API - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. This API may be redistributed unmodified and in full only.
 * For help using the API, and posting your mods, visit the forums at computercraft.info.
 */

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
