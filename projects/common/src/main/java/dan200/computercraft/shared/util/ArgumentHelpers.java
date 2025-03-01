// SPDX-FileCopyrightText: 2020 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.util;

import dan200.computercraft.api.lua.LuaException;
import net.minecraft.ResourceLocationException;
import net.minecraft.core.Registry;
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

    public static <T> T getRegistryEntry(String name, String typeName, Registry<T> registry) throws LuaException {
        ResourceLocation id;
        try {
            id = ResourceLocation.parse(name);
        } catch (ResourceLocationException e) {
            id = null;
        }

        T value;
        if (id == null || (value = registry.get(id)) == null) {
            throw new LuaException(String.format("Unknown %s '%s'", typeName, name));
        }

        return value;
    }
}
