// SPDX-FileCopyrightText: 2025 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.client.platform;

import net.minecraft.client.resources.model.ModelManager;
import org.jspecify.annotations.Nullable;

/**
 * A key used to identify extra/standalone models.
 *
 * @param <T> The type of baked model.
 */
public interface ModelKey<T> {
    /**
     * Lookup this model key in the model manager.
     *
     * @param manager The model manager.
     * @return The loaded model, or {@code null} if not available.
     */
    @Nullable
    T get(ModelManager manager);
}
