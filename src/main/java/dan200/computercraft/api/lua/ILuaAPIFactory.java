/*
 * This file is part of the public ComputerCraft API - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. This API may be redistributed unmodified and in full only.
 * For help using the API, and posting your mods, visit the forums at computercraft.info.
 */
package dan200.computercraft.api.lua;

import dan200.computercraft.api.ComputerCraftAPI;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Construct an {@link ILuaAPI} for a specific computer.
 *
 * @see ILuaAPI
 * @see ComputerCraftAPI#registerAPIFactory(ILuaAPIFactory)
 */
@FunctionalInterface
public interface ILuaAPIFactory
{
    /**
     * Create a new API instance for a given computer.
     *
     * @param computer The computer this API is for.
     * @return The created API, or {@code null} if one should not be injected.
     */
    @Nullable
    ILuaAPI create( @Nonnull IComputerSystem computer );
}
