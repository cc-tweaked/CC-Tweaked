// SPDX-FileCopyrightText: 2021 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.core.util;

import java.util.Collection;

public class LuaUtil {
    public static Object[] consArray(Object value, Collection<?> rest) {
        if (rest.isEmpty()) return new Object[]{ value };

        // I'm not proud of this code.
        var out = new Object[rest.size() + 1];
        out[0] = value;
        var i = 1;
        for (Object additionalType : rest) out[i++] = additionalType;
        return out;
    }
}
