// SPDX-FileCopyrightText: 2021 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.api.lua;

import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.Optional;

import static dan200.computercraft.api.lua.LuaValues.*;

/**
 * A view of a Lua table.
 * <p>
 * Much like {@link IArguments}, this allows for convenient parsing of fields from a Lua table.
 *
 * @param <K> The type of keys in a table, will typically be a wildcard.
 * @param <V> The type of values in a table, will typically be a wildcard.
 * @see ObjectArguments
 */
public abstract class LuaTable<K, V> implements Map<K, V> {
    /**
     * Compute the length of the array part of this table.
     *
     * @return This table's length.
     */
    public int length() {
        var size = 0;
        while (containsKey((double) (size + 1))) size++;
        return size;
    }

    /**
     * Get an array entry as a double.
     *
     * @param index The index in the table, starting at 1.
     * @return The entry's value.
     * @throws LuaException If the value is not a number.
     * @see #getFiniteDouble(int) if you require this to be finite (i.e. not infinite or NaN).
     * @since 1.116
     */
    public double getDouble(int index) throws LuaException {
        Object value = get((double) index);
        if (!(value instanceof Number number)) throw badTableItem(index, "number", getType(value));
        return number.doubleValue();
    }

    /**
     * Get a table entry as a double.
     *
     * @param key The name of the field in the table.
     * @return The field's value.
     * @throws LuaException If the value is not a number.
     * @see #getFiniteDouble(String) if you require this to be finite (i.e. not infinite or NaN).
     * @since 1.116
     */
    public double getDouble(String key) throws LuaException {
        Object value = get(key);
        if (!(value instanceof Number number)) throw badField(key, "number", getType(value));
        return number.doubleValue();
    }

    /**
     * Get an array entry as an integer.
     *
     * @param index The index in the table, starting at 1.
     * @return The entry's value.
     * @throws LuaException If the value is not an integer.
     */
    public long getLong(int index) throws LuaException {
        Object value = get((double) index);
        if (!(value instanceof Number number)) throw badTableItem(index, "number", getType(value));
        checkFiniteIndex(index, number.doubleValue());
        return number.longValue();
    }

    /**
     * Get a table entry as an integer.
     *
     * @param key The name of the field in the table.
     * @return The field's value.
     * @throws LuaException If the value is not an integer.
     */
    public long getLong(String key) throws LuaException {
        Object value = get(key);
        if (!(value instanceof Number number)) throw badField(key, "number", getType(value));
        checkFiniteField(key, number.doubleValue());
        return number.longValue();
    }

    /**
     * Get an array entry as an integer.
     *
     * @param index The index in the table, starting at 1.
     * @return The entry's value.
     * @throws LuaException If the value is not an integer.
     */
    public int getInt(int index) throws LuaException {
        return (int) getLong(index);
    }

    /**
     * Get a table entry as an integer.
     *
     * @param key The name of the field in the table.
     * @return The field's value.
     * @throws LuaException If the value is not an integer.
     */
    public int getInt(String key) throws LuaException {
        return (int) getLong(key);
    }

    /**
     * Get an argument as a finite number (not infinite or NaN).
     *
     * @param index The index in the table, starting at 1.
     * @return The entry's value.
     * @throws LuaException If the value is not finite.
     * @since 1.116
     */
    public double getFiniteDouble(int index) throws LuaException {
        return checkFiniteIndex(index, getDouble(index));
    }

    /**
     * Get an argument as a finite number (not infinite or NaN).
     *
     * @param key The name of the field in the table.
     * @return The field's value.
     * @throws LuaException If the value is not finite.
     * @since 1.116
     */
    public double getFiniteDouble(String key) throws LuaException {
        return checkFiniteField(key, getDouble(key));
    }

    /**
     * Get an array entry as a boolean.
     *
     * @param index The index in the table, starting at 1.
     * @return The entry's value.
     * @throws LuaException If the value is not a boolean.
     * @since 1.116
     */
    public boolean getBoolean(int index) throws LuaException {
        Object value = get((double) index);
        if (!(value instanceof Boolean bool)) throw badTableItem(index, "boolean", getType(value));
        return bool;
    }

    /**
     * Get a table entry as a boolean.
     *
     * @param key The name of the field in the table.
     * @return The field's value.
     * @throws LuaException If the value is not a boolean.
     * @since 1.116
     */
    public boolean getBoolean(String key) throws LuaException {
        Object value = get(key);
        if (!(value instanceof Boolean bool)) throw badField(key, "boolean", getType(value));
        return bool;
    }

    /**
     * Get an array entry as a string.
     *
     * @param index The index in the table, starting at 1.
     * @return The entry's value.
     * @throws LuaException If the value is not a string.
     * @since 1.116
     */
    public String getString(int index) throws LuaException {
        Object value = get((double) index);
        if (!(value instanceof String string)) throw badTableItem(index, "string", getType(value));
        return string;
    }

    /**
     * Get a table entry as a string.
     *
     * @param key The name of the field in the table.
     * @return The field's value.
     * @throws LuaException If the value is not a string.
     * @since 1.116
     */
    public String getString(String key) throws LuaException {
        Object value = get(key);
        if (!(value instanceof String string)) throw badField(key, "string", getType(value));
        return string;
    }

    /**
     * Get an array entry as a table.
     * <p>
     * The returned table may be converted into a {@link LuaTable} (using {@link ObjectLuaTable}) for easier parsing of
     * table keys.
     *
     * @param index The index in the table, starting at 1.
     * @return The entry's value.
     * @throws LuaException If the value is not a table.
     * @since 1.116
     */
    public Map<?, ?> getTable(int index) throws LuaException {
        Object value = get((double) index);
        if (!(value instanceof Map<?, ?> table)) throw badTableItem(index, "table", getType(value));
        return table;
    }

    /**
     * Get a table entry as a table.
     * <p>
     * The returned table may be converted into a {@link LuaTable} (using {@link ObjectLuaTable}) for easier parsing of
     * table keys.
     *
     * @param key The name of the field in the table.
     * @return The field's value.
     * @throws LuaException If the value is not a table.
     * @since 1.116
     */
    public Map<?, ?> getTable(String key) throws LuaException {
        Object value = get(key);
        if (!(value instanceof Map<?, ?> table)) throw badField(key, "table", getType(value));
        return table;
    }

    /**
     * Get an array entry as a double.
     *
     * @param index The index in the table, starting at 1.
     * @return The entry's value, or {@link Optional#empty()} if not present.
     * @throws LuaException If the value is not a number.
     * @see #getFiniteDouble(int) if you require this to be finite (i.e. not infinite or NaN).
     * @since 1.116
     */
    public Optional<Double> optDouble(int index) throws LuaException {
        Object value = get((double) index);
        if (value == null) return Optional.empty();
        if (!(value instanceof Number number)) throw badTableItem(index, "number", getType(value));
        return Optional.of(number.doubleValue());
    }

    /**
     * Get a table entry as a double.
     *
     * @param key The name of the field in the table.
     * @return The field's value, or {@link Optional#empty()} if not present.
     * @throws LuaException If the value is not a number.
     * @see #getFiniteDouble(String) if you require this to be finite (i.e. not infinite or NaN).
     * @since 1.116
     */
    public Optional<Double> optDouble(String key) throws LuaException {
        Object value = get(key);
        if (value == null) return Optional.empty();
        if (!(value instanceof Number number)) throw badField(key, "number", getType(value));
        return Optional.of(number.doubleValue());
    }

    /**
     * Get an array entry as an integer.
     *
     * @param index The index in the table, starting at 1.
     * @return The entry's value, or {@link Optional#empty()} if not present.
     * @throws LuaException If the value is not an integer.
     * @since 1.116
     */
    public Optional<Long> optLong(int index) throws LuaException {
        Object value = get((double) index);
        if (value == null) return Optional.empty();
        if (!(value instanceof Number number)) throw badTableItem(index, "number", getType(value));
        checkFiniteIndex(index, number.doubleValue());
        return Optional.of(number.longValue());
    }

    /**
     * Get a table entry as an integer.
     *
     * @param key The name of the field in the table.
     * @return The field's value, or {@link Optional#empty()} if not present.
     * @throws LuaException If the value is not an integer.
     * @since 1.116
     */
    public Optional<Long> optLong(String key) throws LuaException {
        Object value = get(key);
        if (value == null) return Optional.empty();
        if (!(value instanceof Number number)) throw badField(key, "number", getType(value));
        checkFiniteField(key, number.doubleValue());
        return Optional.of(number.longValue());
    }

    /**
     * Get an array entry as an integer.
     *
     * @param index The index in the table, starting at 1.
     * @return The entry's value, or {@link Optional#empty()} if not present.
     * @throws LuaException If the value is not an integer.
     * @since 1.116
     */
    public Optional<Integer> optInt(int index) throws LuaException {
        return optLong(index).map(Long::intValue);
    }

    /**
     * Get a table entry as an integer.
     *
     * @param key The name of the field in the table.
     * @return The field's value, or {@link Optional#empty()} if not present.
     * @throws LuaException If the value is not an integer.
     * @since 1.116
     */
    public Optional<Integer> optInt(String key) throws LuaException {
        return optLong(key).map(Long::intValue);
    }

    /**
     * Get an argument as a finite number (not infinite or NaN).
     *
     * @param index The index in the table, starting at 1.
     * @return The entry's value, or {@link Optional#empty()} if not present.
     * @throws LuaException If the value is not finite.
     * @since 1.116
     */
    public Optional<Double> optFiniteDouble(int index) throws LuaException {
        var value = optDouble(index);
        if (value.isPresent()) checkFiniteIndex(index, value.get());
        return value;
    }

    /**
     * Get an argument as a finite number (not infinite or NaN).
     *
     * @param key The name of the field in the table.
     * @return The field's value, or {@link Optional#empty()} if not present.
     * @throws LuaException If the value is not finite.
     * @since 1.116
     */
    public Optional<Double> optFiniteDouble(String key) throws LuaException {
        var value = optDouble(key);
        if (value.isPresent()) checkFiniteField(key, value.get());
        return value;
    }

    /**
     * Get an array entry as a boolean.
     *
     * @param index The index in the table, starting at 1.
     * @return The entry's value, or {@link Optional#empty()} if not present.
     * @throws LuaException If the value is not a boolean.
     * @since 1.116
     */
    public Optional<Boolean> optBoolean(int index) throws LuaException {
        Object value = get((double) index);
        if (value == null) return Optional.empty();
        if (!(value instanceof Boolean bool)) throw badTableItem(index, "boolean", getType(value));
        return Optional.of(bool);
    }

    /**
     * Get a table entry as a boolean.
     *
     * @param key The name of the field in the table.
     * @return The field's value, or {@link Optional#empty()} if not present.
     * @throws LuaException If the value is not a boolean.
     * @since 1.116
     */
    public Optional<Boolean> optBoolean(String key) throws LuaException {
        Object value = get(key);
        if (value == null) return Optional.empty();
        if (!(value instanceof Boolean bool)) throw badField(key, "boolean", getType(value));
        return Optional.of(bool);
    }

    /**
     * Get an array entry as a double.
     *
     * @param index The index in the table, starting at 1.
     * @return The entry's value, or {@link Optional#empty()} if not present.
     * @throws LuaException If the value is not a string.
     * @since 1.116
     */
    public Optional<String> optString(int index) throws LuaException {
        Object value = get((double) index);
        if (value == null) return Optional.empty();
        if (!(value instanceof String string)) throw badTableItem(index, "string", getType(value));
        return Optional.of(string);
    }

    /**
     * Get a table entry as a string.
     *
     * @param key The name of the field in the table.
     * @return The field's value, or {@link Optional#empty()} if not present.
     * @throws LuaException If the value is not a string.
     * @since 1.116
     */
    public Optional<String> optString(String key) throws LuaException {
        Object value = get(key);
        if (value == null) return Optional.empty();
        if (!(value instanceof String string)) throw badField(key, "string", getType(value));
        return Optional.of(string);
    }

    /**
     * Get an array entry as a table.
     * <p>
     * The returned table may be converted into a {@link LuaTable} (using {@link ObjectLuaTable}) for easier parsing of
     * table keys.
     *
     * @param index The index in the table, starting at 1.
     * @return The entry's value, or {@link Optional#empty()} if not present.
     * @throws LuaException If the value is not a table.
     * @since 1.116
     */
    public Optional<Map<?, ?>> optTable(int index) throws LuaException {
        Object value = get((double) index);
        if (value == null) return Optional.empty();
        if (!(value instanceof Map<?, ?> table)) throw badTableItem(index, "table", getType(value));
        return Optional.of(table);
    }

    /**
     * Get a table entry as a table.
     * <p>
     * The returned table may be converted into a {@link LuaTable} (using {@link ObjectLuaTable}) for easier parsing of
     * table keys.
     *
     * @param key The name of the field in the table.
     * @return The field's value, or {@link Optional#empty()} if not present.
     * @throws LuaException If the value is not a table.
     * @since 1.116
     */
    public Optional<Map<?, ?>> optTable(String key) throws LuaException {
        Object value = get(key);
        if (value == null) return Optional.empty();
        if (!(value instanceof Map<?, ?> table)) throw badField(key, "table", getType(value));
        return Optional.of(table);
    }

    @Nullable
    @Override
    public V put(K o, V o2) {
        throw new UnsupportedOperationException("Cannot modify LuaTable");
    }

    @Override
    public V remove(Object o) {
        throw new UnsupportedOperationException("Cannot modify LuaTable");
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> map) {
        throw new UnsupportedOperationException("Cannot modify LuaTable");
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException("Cannot modify LuaTable");
    }
}
