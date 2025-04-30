// SPDX-FileCopyrightText: 2025 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.client.platform;

import net.minecraft.client.resources.model.ModelManager;
import net.neoforged.neoforge.client.model.standalone.StandaloneModelKey;
import org.jspecify.annotations.Nullable;

/**
 * Wraps a Forge {@link StandaloneModelKey} into our multi-loader {@link ModelKey}.
 *
 * @param <T> The type of the baked model.
 */
public final class ForgeModelKey<T> implements ModelKey<T> {
    private final StandaloneModelKey<T> key;

    ForgeModelKey(StandaloneModelKey<T> key) {
        this.key = key;
    }

    public static <T> StandaloneModelKey<T> key(ModelKey<T> key) {
        return ((ForgeModelKey<T>) key).key;
    }

    @SuppressWarnings("unchecked")
    public static <T> StandaloneModelKey<T> erased(ModelKey<?> key) {
        return ((ForgeModelKey<T>) key).key;
    }

    @Override
    public @Nullable T get(ModelManager manager) {
        return manager.getStandaloneModel(key);
    }
}
