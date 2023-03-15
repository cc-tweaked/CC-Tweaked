// SPDX-FileCopyrightText: 2021 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.api.lua;

import javax.annotation.Nullable;
import java.util.Map;

import static dan200.computercraft.api.lua.LuaValues.*;

/**
 * A view of a Lua table, which may be able to access table elements in a more optimised manner than
 * {@link IArguments#getTable(int)}.
 *
 * @param <K> The type of keys in a table, will typically be a wildcard.
 * @param <V> The type of values in a table, will typically be a wildcard.
 */
public interface LuaTable<K, V> extends Map<K, V> {
    /**
     * Compute the length of the array part of this table.
     *
     * @return This table's length.
     */
    default int length() {
        var size = 0;
        while (containsKey((double) (size + 1))) size++;
        return size;
    }

    /**
     * Get an array entry as an integer.
     *
     * @param index The index in the table, starting at 1.
     * @return The table's value.
     * @throws LuaException If the value is not an integer.
     */
    default long getLong(int index) throws LuaException {
        Object value = get((double) index);
        if (!(value instanceof Number number)) throw badTableItem(index, "number", getType(value));

        var asDouble = number.doubleValue();
        if (!Double.isFinite(asDouble)) throw badTableItem(index, "number", getNumericType(asDouble));
        return number.longValue();
    }

    /**
     * Get a table entry as an integer.
     *
     * @param key The name of the field in the table.
     * @return The table's value.
     * @throws LuaException If the value is not an integer.
     */
    default long getLong(String key) throws LuaException {
        Object value = get(key);
        if (!(value instanceof Number number)) throw badField(key, "number", getType(value));

        var asDouble = number.doubleValue();
        if (!Double.isFinite(asDouble)) throw badField(key, "number", getNumericType(asDouble));
        return number.longValue();
    }

    /**
     * Get an array entry as an integer.
     *
     * @param index The index in the table, starting at 1.
     * @return The table's value.
     * @throws LuaException If the value is not an integer.
     */
    default int getInt(int index) throws LuaException {
        return (int) getLong(index);
    }

    /**
     * Get a table entry as an integer.
     *
     * @param key The name of the field in the table.
     * @return The table's value.
     * @throws LuaException If the value is not an integer.
     */
    default int getInt(String key) throws LuaException {
        return (int) getLong(key);
    }

    @Nullable
    @Override
    default V put(K o, V o2) {
        throw new UnsupportedOperationException("Cannot modify LuaTable");
    }

    @Override
    default V remove(Object o) {
        throw new UnsupportedOperationException("Cannot modify LuaTable");
    }

    @Override
    default void putAll(Map<? extends K, ? extends V> map) {
        throw new UnsupportedOperationException("Cannot modify LuaTable");
    }

    @Override
    default void clear() {
        throw new UnsupportedOperationException("Cannot modify LuaTable");
    }
}
