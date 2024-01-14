// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.platform;

import net.minecraft.core.Direction;

import javax.annotation.Nullable;

/**
 * A (possibly cached) provider of a component at a specific location.
 *
 * @param <T> The type of the component.
 */
public interface ComponentAccess<T> {
    /**
     * Get a peripheral for the current block.
     *
     * @param direction The direction the peripheral is in.
     * @return The peripheral, or {@literal null} if not found.
     * @throws IllegalStateException If the level or position have changed.
     */
    @Nullable
    T get(Direction direction);
}
