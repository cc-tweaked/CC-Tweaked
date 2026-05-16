// SPDX-FileCopyrightText: 2026 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.core.lua;

import org.squiddev.cobalt.*;

import java.util.Map;

class CobaltLuaTableTest implements LuaTableContract<TableImpl> {
    @Override
    public TableImpl create(Map<?, ?> map) {
        try {
            return new TableImpl(VarargArguments.of(Constants.NONE), convertMap(map));
        } catch (LuaError e) {
            throw new RuntimeException(e);
        }
    }

    private static LuaValue convert(Object object) throws LuaError {
        var value = TableImpl.convertValue(object);
        if (value != null) return value;

        if (object instanceof Map<?, ?> x) return convertMap(x);
        if (object instanceof Integer x) return ValueFactory.valueOf(x);
        throw new IllegalArgumentException("Unknown value " + object);
    }

    private static LuaTable convertMap(Map<?, ?> map) throws LuaError {
        var out = new LuaTable();
        for (var entry : map.entrySet()) out.rawset(convert(entry.getKey()), convert(entry.getValue()));
        return out;
    }
}
