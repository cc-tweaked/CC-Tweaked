/*
 * This file is part of the public ComputerCraft API - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2018. This API may be redistributed unmodified and in full only.
 * For help using the API, and posting your mods, visit the forums at computercraft.info.
 */

package dan200.computercraft.api.lua;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * A function which executes using a {@link ILuaContext}.
 *
 * Like {@link ILuaContext}, this is not intended for use in the future - it purely exists as an argument for
 * {@link MethodResult#withLuaContext(ILuaContextTask)}.
 */
@FunctionalInterface
public interface ILuaContextTask
{
    @Nullable
    @Deprecated
    Object[] execute( @Nonnull ILuaContext context ) throws LuaException, InterruptedException;
}
