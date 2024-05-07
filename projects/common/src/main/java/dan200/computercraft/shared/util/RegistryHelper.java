// SPDX-FileCopyrightText: 2023 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.util;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.ApiStatus;

/**
 * Additioanl functions for working with {@linkplain Registry registries}.
 */
@ApiStatus.Internal
public final class RegistryHelper {
    private RegistryHelper() {
    }

    /**
     * Find a registry from a {@link ResourceKey}, throwing if it does not exist.
     *
     * @param id  The id of the registry.
     * @param <T> The contents of the registry
     * @return The associated registry.
     */
    @SuppressWarnings("unchecked")
    public static <T> Registry<T> getRegistry(ResourceKey<Registry<T>> id) {
        var registry = (Registry<T>) BuiltInRegistries.REGISTRY.get(id.location());
        if (registry == null) throw new IllegalArgumentException("Unknown registry " + id);
        return registry;
    }

    /**
     * Get the key of a registry entry, throwing if it is not registered.
     *
     * @param registry The registry to look up in.
     * @param object   The object to look up.
     * @param <T>      The type of this registry.
     * @return The ID of this object
     * @see Registry#getResourceKey(Object)
     */
    public static <T> ResourceLocation getKeyOrThrow(Registry<T> registry, T object) {
        var key = registry.getResourceKey(object);
        if (key.isEmpty()) throw new IllegalArgumentException(object + " was not registered in " + registry.key());
        return key.get().location();
    }
}
