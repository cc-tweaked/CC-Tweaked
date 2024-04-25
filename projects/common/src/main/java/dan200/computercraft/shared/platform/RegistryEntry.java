// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.platform;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Supplier;

/**
 * A reference to an entry in a {@link Registry}.
 *
 * @param <U> The type of the object registered.
 * @see RegistrationHelper
 */
public interface RegistryEntry<U> extends Supplier<U> {
    /**
     * Get the ID of this registered item.
     *
     * @return This registered item.
     */
    ResourceLocation id();

    static <T> Codec<RegistryEntry<? extends T>> codec(Registry<T> registry) {
        record HolderEntry<T>(ResourceLocation id, Holder<T> holder) implements RegistryEntry<T> {
            @Override
            public T get() {
                return holder().value();
            }
        }

        return ResourceLocation.CODEC.flatXmap(
            id -> registry
                .getHolder(ResourceKey.create(registry.key(), id))
                .map(x -> DataResult.success(new HolderEntry<>(id, x)))
                .orElseGet(() -> DataResult.error(() -> "Unknown registry key in " + registry.key() + ": " + id)),
            holder -> DataResult.success(holder.id())
        );
    }
}
