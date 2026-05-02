// SPDX-FileCopyrightText: 2026 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.core.lua;

import dan200.computercraft.api.lua.ObjectLuaTable;

import java.util.Map;

class ObjectLuaTableTest implements LuaTableContract<ObjectLuaTable> {
    @Override
    public ObjectLuaTable create(Map<?, ?> map) {
        return new ObjectLuaTable(map);
    }
}
