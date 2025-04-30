// SPDX-FileCopyrightText: 2025 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.client.platform;

import net.fabricmc.fabric.api.client.model.loading.v1.ExtraModelKey;
import net.minecraft.client.resources.model.ModelManager;
import org.jspecify.annotations.Nullable;

/**
 * Wraps a Fabric {@link ExtraModelKey} into our multi-loader {@link ModelKey}.
 *
 * @param <T> The type of the baked model.
 */
public final class FabricModelKey<T> implements ModelKey<T> {
    private final ExtraModelKey<T> key;

    FabricModelKey(ExtraModelKey<T> key) {
        this.key = key;
    }

    public static <T> ExtraModelKey<T> key(ModelKey<T> key) {
        return ((FabricModelKey<T>) key).key;
    }

    @SuppressWarnings("unchecked")
    public static <T> ExtraModelKey<T> erased(ModelKey<?> key) {
        return ((FabricModelKey<T>) key).key;
    }

    @Override
    public @Nullable T get(ModelManager manager) {
        return manager.getModel(key);
    }
}
