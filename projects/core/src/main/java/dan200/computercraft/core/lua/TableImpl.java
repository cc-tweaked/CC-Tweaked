// SPDX-FileCopyrightText: 2021 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.core.lua;

import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaValues;
import org.jetbrains.annotations.VisibleForTesting;
import org.jspecify.annotations.Nullable;
import org.squiddev.cobalt.*;

import java.nio.ByteBuffer;
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
        checkValid();
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
        var value = convertValue(o);
        return value == null ? Constants.NIL : table.rawget(value);
    }

    @VisibleForTesting
    static @Nullable LuaValue convertValue(@Nullable Object object) {
        if (object == null) return Constants.NIL;
        if (object instanceof Boolean bool) return ValueFactory.valueOf(bool);
        if (object instanceof Double num) return ValueFactory.valueOf(num);
        if (object instanceof String str) return ValueFactory.valueOf(str);
        if (object instanceof byte[] b) return ValueFactory.valueOf(Arrays.copyOf(b, b.length));
        if (object instanceof ByteBuffer b) {
            var bytes = new byte[b.remaining()];
            b.get(bytes);
            return ValueFactory.valueOf(bytes);
        }

        return null;
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

    @Override
    public @Nullable Object get(int index) {
        checkValid();
        return CobaltLuaMachine.toObject(table.rawget(index), null);
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
