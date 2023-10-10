// SPDX-FileCopyrightText: 2020 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.util;

import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.shared.platform.RegistryWrappers;
import net.minecraft.ResourceLocationException;
import net.minecraft.resources.ResourceLocation;

/**
 * A few helpers for working with arguments.
 * <p>
 * This should really be moved into the public API. However, until I have settled on a suitable format, we'll keep it
 * where it is used.
 */
public final class ArgumentHelpers {
    private ArgumentHelpers() {
    }

    public static <T> T getRegistryEntry(String name, String typeName, RegistryWrappers.RegistryWrapper<T> registry) throws LuaException {
        ResourceLocation id;
        try {
            id = new ResourceLocation(name);
        } catch (ResourceLocationException e) {
            id = null;
        }

        T value;
        if (id == null || (value = registry.tryGet(id)) == null) {
            throw new LuaException(String.format("Unknown %s '%s'", typeName, name));
        }

        return value;
    }
}
