// Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
//
// SPDX-License-Identifier: LicenseRef-CCPL

package dan200.computercraft.shared.details;

import dan200.computercraft.api.detail.BlockReference;
import dan200.computercraft.shared.platform.RegistryWrappers;
import net.minecraft.world.level.block.state.properties.Property;

import java.util.HashMap;
import java.util.Map;

public class BlockDetails {
    public static void fillBasic(Map<? super String, Object> data, BlockReference block) {
        var state = block.state();

        data.put("name", DetailHelpers.getId(RegistryWrappers.BLOCKS, state.getBlock()));

        Map<Object, Object> stateTable = new HashMap<>();
        for (Map.Entry<Property<?>, ? extends Comparable<?>> entry : state.getValues().entrySet()) {
            var property = entry.getKey();
            stateTable.put(property.getName(), getPropertyValue(property, entry.getValue()));
        }
        data.put("state", stateTable);
    }

    public static void fill(Map<? super String, Object> data, BlockReference block) {
        data.put("tags", DetailHelpers.getTags(block.state().getTags()));
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static Object getPropertyValue(Property property, Comparable value) {
        if (value instanceof String || value instanceof Number || value instanceof Boolean) return value;
        return property.getName(value);
    }
}
