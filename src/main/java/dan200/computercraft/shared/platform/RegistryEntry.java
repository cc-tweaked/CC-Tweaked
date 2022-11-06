/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
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
