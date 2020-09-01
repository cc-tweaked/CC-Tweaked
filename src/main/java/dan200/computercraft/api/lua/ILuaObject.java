/*
 * This file is part of the public ComputerCraft API - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. This API may be redistributed unmodified and in full only.
 * For help using the API, and posting your mods, visit the forums at computercraft.info.
 */

package dan200.computercraft.api.lua;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import dan200.computercraft.api.peripheral.IComputerAccess;
import dan200.computercraft.api.peripheral.IPeripheral;

public interface ILuaObject {
    @Nonnull
    String[] getMethodNames();

    @Nullable
    Object[] callMethod(@Nonnull ILuaContext context, int method, @Nonnull Object[] arguments) throws LuaException, InterruptedException;
}
