// SPDX-FileCopyrightText: 2021 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.core.util;

import dan200.computercraft.core.lua.ILuaMachine;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class LuaUtil {
    private static final List<?> EMPTY_LIST = List.of();
    private static final Set<?> EMPTY_SET = Set.of();
    private static final Map<?, ?> EMPTY_MAP = Map.of();

    public static Object[] consArray(Object value, Collection<?> rest) {
        if (rest.isEmpty()) return new Object[]{ value };

        // I'm not proud of this code.
        var out = new Object[rest.size() + 1];
        out[0] = value;
        var i = 1;
        for (var additionalType : rest) out[i++] = additionalType;
        return out;
    }

    /**
     * Determine whether a value is a singleton collection, such as one created with {@link List#of()}.
     * <p>
     * These collections are treated specially by {@link ILuaMachine} implementations: we skip sharing for them, and
     * create a new table each time.
     *
     * @param value The value to test.
     * @return Whether this is a singleton collection.
     */
    public static boolean isSingletonCollection(Object value) {
        return value == EMPTY_LIST || value == EMPTY_SET || value == EMPTY_MAP;
    }
}
