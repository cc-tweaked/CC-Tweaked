// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.platform;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;

import java.util.function.Supplier;

/**
 * Provides a utility for registering entries in Minecraft's {@linkplain Registry registries}.
 * <p>
 * This is similar to Forge's {@code DeferredRegistry}: registration does not happen immediately, instead a reference
 * to the registered item is returned (see {@link RegistryEntry}). When registration does occur (due to an event or mod
 * initialisation), the entry is created and added to the registry.
 *
 * @param <T> The parent type of all objects in the registry, such as {@link Block}
 * @see PlatformHelper#createRegistrationHelper(ResourceKey) Obtain a new instance of this interface.
 */
public interface RegistrationHelper<T> {
    /**
     * Register an entry in this helper. This does <em>NOT</em> immediately register the object in the underlying
     * {@link Registry}.
     *
     * @param name   The name of this entry.
     * @param create A factory method to create the entry.
     * @param <U>    The type of this item in the registry.
     * @return The {@link RegistryEntry} for the registered entry.
     */
    <U extends T> RegistryEntry<U> register(String name, Supplier<U> create);

    /**
     * Register this helper.
     */
    void register();
}
