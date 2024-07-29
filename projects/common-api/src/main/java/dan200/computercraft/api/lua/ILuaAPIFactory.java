// SPDX-FileCopyrightText: 2017 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.api.lua;

import dan200.computercraft.api.ComputerCraftAPI;

import javax.annotation.Nullable;

/**
 * Construct an {@link ILuaAPI} for a computer.
 *
 * @see ILuaAPI
 * @see ComputerCraftAPI#registerAPIFactory(ILuaAPIFactory)
 */
@FunctionalInterface
public interface ILuaAPIFactory {
    /**
     * Create a new API instance for a given computer.
     *
     * @param computer The computer this API is for.
     * @return The created API, or {@code null} if one should not be injected.
     */
    @Nullable
    ILuaAPI create(IComputerSystem computer);
}
