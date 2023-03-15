// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.platform;

import net.minecraft.core.Registry;
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
}
