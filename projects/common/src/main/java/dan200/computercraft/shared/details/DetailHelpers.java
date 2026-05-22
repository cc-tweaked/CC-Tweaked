// SPDX-FileCopyrightText: 2020 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.details;

import dan200.computercraft.shared.platform.RegistryWrappers;
import dan200.computercraft.shared.util.ARGB32;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;

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

    public static void fillMapColour(Map<? super String, Object> data, BlockGetter level, BlockPos pos, BlockState state) {
        var mapColour = state.getMapColor(level, pos);
        if (mapColour == MapColor.NONE) return;

        var colour = ARGB32.transparent(mapColour.col);
        data.put("mapColor", colour);
        data.put("mapColour", colour);
    }
}
