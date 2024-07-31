// SPDX-FileCopyrightText: 2018 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.impl;

import dan200.computercraft.api.lua.ILuaAPIFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;

/**
 * The global factory for {@link ILuaAPIFactory}s.
 *
 * @see dan200.computercraft.api.ComputerCraftAPI#registerAPIFactory(ILuaAPIFactory)
 */
public final class ApiFactories {
    private ApiFactories() {
    }

    private static final Collection<ILuaAPIFactory> factories = new LinkedHashSet<>();

    static synchronized void register(ILuaAPIFactory factory) {
        Objects.requireNonNull(factory, "provider cannot be null");
        factories.add(factory);
    }

    public static Collection<ILuaAPIFactory> getAll() {
        return Collections.unmodifiableCollection(factories);
    }
}
