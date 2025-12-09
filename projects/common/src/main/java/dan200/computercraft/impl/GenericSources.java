// SPDX-FileCopyrightText: 2023 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.impl;

import dan200.computercraft.api.lua.GenericSource;
import dan200.computercraft.core.asm.GenericMethod;
import dan200.computercraft.shared.config.ConfigSpec;
import net.minecraft.server.MinecraftServer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

/**
 * The global registry for {@link GenericSource}s.
 *
 * @see dan200.computercraft.core.ComputerContext.Builder#genericMethods(Collection)
 * @see dan200.computercraft.api.ComputerCraftAPI#registerGenericSource(GenericSource)
 */
public final class GenericSources {
    private GenericSources() {
    }

    private static final Collection<Function<MinecraftServer, GenericSource>> sources = new ArrayList<>();

    static synchronized void register(Function<MinecraftServer, GenericSource> source) {
        Objects.requireNonNull(source, "provider cannot be null");
        sources.add(source);
    }

    public static Collection<GenericMethod> getAllMethods(MinecraftServer server) {
        var disabledMethods = Set.copyOf(ConfigSpec.disabledGenericMethods.get());
        return sources.stream()
            .map(x -> x.apply(server))
            .filter(x -> !disabledMethods.contains(x.id()))
            .flatMap(GenericMethod::getMethods)
            .filter(x -> !disabledMethods.contains(x.id()))
            .toList();
    }
}
