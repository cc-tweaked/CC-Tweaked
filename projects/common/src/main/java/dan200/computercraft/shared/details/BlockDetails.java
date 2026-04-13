// Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
//
// SPDX-License-Identifier: LicenseRef-CCPL

package dan200.computercraft.shared.details;

import dan200.computercraft.api.detail.BlockReference;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.state.properties.Property;

import java.util.Map;
import java.util.stream.Collectors;

public class BlockDetails {
    public static void fillBasic(Map<? super String, Object> data, BlockReference block) {
        var state = block.state();

        data.put("name", DetailHelpers.getId(BuiltInRegistries.BLOCK, state.getBlock()));

        data.put("state", state.getValues().collect(Collectors.toMap(
            x -> x.property().getName(),
            x -> getPropertyValue(x.property(), x.value())
        )));
    }

    public static void fill(Map<? super String, Object> data, BlockReference block) {
        data.put("tags", DetailHelpers.getTags(block.state()));
        DetailHelpers.fillMapColour(data, block.level(), block.pos(), block.state());
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static Object getPropertyValue(Property property, Comparable value) {
        if (value instanceof String || value instanceof Number || value instanceof Boolean) return value;
        return property.getName(value);
    }
}
