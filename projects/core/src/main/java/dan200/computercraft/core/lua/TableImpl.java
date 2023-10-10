// SPDX-FileCopyrightText: 2021 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.core.lua;

import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaValues;
import org.squiddev.cobalt.*;

import javax.annotation.Nullable;
import java.util.*;

import static dan200.computercraft.api.lua.LuaValues.badTableItem;
import static dan200.computercraft.api.lua.LuaValues.getNumericType;

class TableImpl implements dan200.computercraft.api.lua.LuaTable<Object, Object> {
    private final VarargArguments arguments;
    private final LuaTable table;
    private @Nullable Map<Object, Object> backingMap;

    TableImpl(VarargArguments arguments, LuaTable table) {
        this.arguments = arguments;
        this.table = table;
    }

    @Override
    public int size() {
        checkValid();
        return table.size();
    }

    @Override
    public int length() {
        return table.length();
    }

    @Override
    public long getLong(int index) throws LuaException {
        var value = table.rawget(index);
        if (!(value instanceof LuaNumber)) throw LuaValues.badTableItem(index, "number", value.typeName());
        if (value instanceof LuaInteger) return value.toInteger();

        var number = value.toDouble();
        if (!Double.isFinite(number)) throw badTableItem(index, "number", getNumericType(number));
        return (long) number;
    }

    @Override
    public boolean isEmpty() {
        checkValid();
        try {
            return table.next(Constants.NIL).first().isNil();
        } catch (LuaError e) {
            throw new IllegalStateException(e);
        }
    }

    private LuaValue getImpl(Object o) {
        checkValid();
        if (o instanceof String) return table.rawget((String) o);
        if (o instanceof Integer) return table.rawget((Integer) o);
        return Constants.NIL;
    }

    @Override
    public boolean containsKey(Object o) {
        return !getImpl(o).isNil();
    }

    @Nullable
    @Override
    public Object get(Object o) {
        return CobaltLuaMachine.toObject(getImpl(o), null);
    }

    private Map<Object, Object> getBackingMap() {
        checkValid();
        if (backingMap != null) return backingMap;
        return backingMap = Collections.unmodifiableMap(
            Objects.requireNonNull((Map<?, ?>) CobaltLuaMachine.toObject(table, null))
        );
    }

    @Override
    public boolean containsValue(Object o) {
        return getBackingMap().containsKey(o);
    }

    @Override
    public Set<Object> keySet() {
        return getBackingMap().keySet();
    }

    @Override
    public Collection<Object> values() {
        return getBackingMap().values();
    }

    @Override
    public Set<Entry<Object, Object>> entrySet() {
        return getBackingMap().entrySet();
    }

    private void checkValid() {
        if (arguments.isClosed()) {
            throw new IllegalStateException("Cannot use LuaTable after IArguments has been released");
        }
    }
}
