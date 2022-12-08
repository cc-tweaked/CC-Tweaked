/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.details;

import dan200.computercraft.shared.platform.RegistryWrappers;
import net.minecraft.core.Holder;
import net.minecraft.tags.TagKey;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class DetailHelpers {
    private DetailHelpers() {
    }

    public static <T> Map<String, Boolean> getTags(Holder.Reference<T> object) {
        return getTags(object.tags());
    }

    public static <T> Map<String, Boolean> getTags(Stream<TagKey<T>> tags) {
        return tags.collect(Collectors.toMap(x -> x.location().toString(), x -> true));
    }

    public static <T> String getId(RegistryWrappers.RegistryWrapper<T> registry, T entry) {
        return registry.getKey(entry).toString();
    }
}
