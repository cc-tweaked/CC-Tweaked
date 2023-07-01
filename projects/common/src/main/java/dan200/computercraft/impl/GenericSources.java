// SPDX-FileCopyrightText: 2023 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.impl;

import dan200.computercraft.api.lua.GenericSource;
import dan200.computercraft.core.asm.GenericMethod;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Objects;

/**
 * The global registry for {@link GenericSource}s.
 *
 * @see dan200.computercraft.core.ComputerContext.Builder#genericMethods(Collection)
 * @see dan200.computercraft.api.ComputerCraftAPI#registerGenericSource(GenericSource)
 */
public final class GenericSources {
    private GenericSources() {
    }

    private static final Collection<GenericSource> sources = new LinkedHashSet<>();

    static synchronized void register(GenericSource source) {
        Objects.requireNonNull(source, "provider cannot be null");
        sources.add(source);
    }

    public static Collection<GenericMethod> getAllMethods() {
        return sources.stream().flatMap(GenericMethod::getMethods).toList();
    }
}
