// SPDX-FileCopyrightText: 2020 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.api.lua;

import org.jetbrains.annotations.Contract;

import javax.annotation.Nullable;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.Optional;

/**
 * The arguments passed to a function.
 */
public interface IArguments {
    /**
     * Get the number of arguments passed to this function.
     *
     * @return The number of passed arguments.
     */
    int count();

    /**
     * Get the argument at the specific index. The returned value must obey the following conversion rules:
     *
     * <ul>
     *   <li>Lua values of type "string" will be represented by a {@link String}.</li>
     *   <li>Lua values of type "number" will be represented by a {@link Number}.</li>
     *   <li>Lua values of type "boolean" will be represented by a {@link Boolean}.</li>
     *   <li>Lua values of type "table" will be represented by a {@link Map}.</li>
     *   <li>Lua values of any other type will be represented by a {@code null} value.</li>
     * </ul>
     *
     * @param index The argument number.
     * @return The argument's value, or {@code null} if not present.
     */
    @Nullable
    Object get(int index);

    /**
     * Drop a number of arguments. The returned arguments instance will access arguments at position {@code i + count},
     * rather than {@code i}. However, errors will still use the given argument index.
     *
     * @param count The number of arguments to drop.
     * @return The new {@link IArguments} instance.
     */
    IArguments drop(int count);

    default Object[] getAll() {
        var result = new Object[count()];
        for (var i = 0; i < result.length; i++) result[i] = get(i);
        return result;
    }

    /**
     * Get an argument as a double.
     *
     * @param index The argument number.
     * @return The argument's value.
     * @throws LuaException If the value is not a number.
     * @see #getFiniteDouble(int) if you require this to be finite (i.e. not infinite or NaN).
     */
    default double getDouble(int index) throws LuaException {
        var value = get(index);
        if (!(value instanceof Number number)) throw LuaValues.badArgumentOf(index, "number", value);
        return number.doubleValue();
    }

    /**
     * Get an argument as an integer.
     *
     * @param index The argument number.
     * @return The argument's value.
     * @throws LuaException If the value is not an integer.
     */
    default int getInt(int index) throws LuaException {
        return (int) getLong(index);
    }

    /**
     * Get an argument as a long.
     *
     * @param index The argument number.
     * @return The argument's value.
     * @throws LuaException If the value is not a long.
     */
    default long getLong(int index) throws LuaException {
        var value = get(index);
        if (!(value instanceof Number number)) throw LuaValues.badArgumentOf(index, "number", value);
        return LuaValues.checkFiniteNum(index, number).longValue();
    }

    /**
     * Get an argument as a finite number (not infinite or NaN).
     *
     * @param index The argument number.
     * @return The argument's value.
     * @throws LuaException If the value is not finite.
     */
    default double getFiniteDouble(int index) throws LuaException {
        return LuaValues.checkFinite(index, getDouble(index));
    }

    /**
     * Get an argument as a boolean.
     *
     * @param index The argument number.
     * @return The argument's value.
     * @throws LuaException If the value is not a boolean.
     */
    default boolean getBoolean(int index) throws LuaException {
        var value = get(index);
        if (!(value instanceof Boolean bool)) throw LuaValues.badArgumentOf(index, "boolean", value);
        return bool;
    }

    /**
     * Get an argument as a string.
     *
     * @param index The argument number.
     * @return The argument's value.
     * @throws LuaException If the value is not a string.
     */
    default String getString(int index) throws LuaException {
        var value = get(index);
        if (!(value instanceof String string)) throw LuaValues.badArgumentOf(index, "string", value);
        return string;
    }

    /**
     * Get a string argument as a byte array.
     *
     * @param index The argument number.
     * @return The argument's value. This is a <em>read only</em> buffer.
     * @throws LuaException If the value is not a string.
     */
    default ByteBuffer getBytes(int index) throws LuaException {
        return LuaValues.encode(getString(index));
    }

    /**
     * Get a string argument as an enum value.
     *
     * @param index The argument number.
     * @param klass The type of enum to parse.
     * @param <T>   The type of enum to parse.
     * @return The argument's value.
     * @throws LuaException If the value is not a string or not a valid option for this enum.
     */
    default <T extends Enum<T>> T getEnum(int index, Class<T> klass) throws LuaException {
        return LuaValues.checkEnum(index, klass, getString(index));
    }

    /**
     * Get an argument as a table.
     *
     * @param index The argument number.
     * @return The argument's value.
     * @throws LuaException If the value is not a table.
     */
    default Map<?, ?> getTable(int index) throws LuaException {
        var value = get(index);
        if (!(value instanceof Map)) throw LuaValues.badArgumentOf(index, "table", value);
        return (Map<?, ?>) value;
    }

    /**
     * Get an argument as a table in an unsafe manner.
     * <p>
     * Classes implementing this interface may choose to implement a more optimised version which does not copy the
     * table, instead returning a wrapper version, making it more efficient. However, the caller must guarantee that
     * they do not access the table the computer thread (and so should not be used with main-thread functions) or once
     * the initial call has finished (for instance, in a callback to {@link MethodResult#pullEvent}).
     *
     * @param index The argument number.
     * @return The argument's value.
     * @throws LuaException If the value is not a table.
     */
    default LuaTable<?, ?> getTableUnsafe(int index) throws LuaException {
        return new ObjectLuaTable(getTable(index));
    }

    /**
     * Get an argument as a double.
     *
     * @param index The argument number.
     * @return The argument's value, or {@link Optional#empty()} if not present.
     * @throws LuaException If the value is not a number.
     */
    default Optional<Double> optDouble(int index) throws LuaException {
        var value = get(index);
        if (value == null) return Optional.empty();
        if (!(value instanceof Number number)) throw LuaValues.badArgumentOf(index, "number", value);
        return Optional.of(number.doubleValue());
    }

    /**
     * Get an argument as an int.
     *
     * @param index The argument number.
     * @return The argument's value, or {@link Optional#empty()} if not present.
     * @throws LuaException If the value is not a number.
     */
    default Optional<Integer> optInt(int index) throws LuaException {
        return optLong(index).map(Long::intValue);
    }

    /**
     * Get an argument as a long.
     *
     * @param index The argument number.
     * @return The argument's value, or {@link Optional#empty()} if not present.
     * @throws LuaException If the value is not a number.
     */
    default Optional<Long> optLong(int index) throws LuaException {
        var value = get(index);
        if (value == null) return Optional.empty();
        if (!(value instanceof Number number)) throw LuaValues.badArgumentOf(index, "number", value);
        return Optional.of(LuaValues.checkFiniteNum(index, number).longValue());
    }

    /**
     * Get an argument as a finite number (not infinite or NaN).
     *
     * @param index The argument number.
     * @return The argument's value, or {@link Optional#empty()} if not present.
     * @throws LuaException If the value is not finite.
     */
    default Optional<Double> optFiniteDouble(int index) throws LuaException {
        var value = optDouble(index);
        if (value.isPresent()) LuaValues.checkFiniteNum(index, value.get());
        return value;
    }

    /**
     * Get an argument as a boolean.
     *
     * @param index The argument number.
     * @return The argument's value, or {@link Optional#empty()} if not present.
     * @throws LuaException If the value is not a boolean.
     */
    default Optional<Boolean> optBoolean(int index) throws LuaException {
        var value = get(index);
        if (value == null) return Optional.empty();
        if (!(value instanceof Boolean bool)) throw LuaValues.badArgumentOf(index, "boolean", value);
        return Optional.of(bool);
    }

    /**
     * Get an argument as a string.
     *
     * @param index The argument number.
     * @return The argument's value, or {@link Optional#empty()} if not present.
     * @throws LuaException If the value is not a string.
     */
    default Optional<String> optString(int index) throws LuaException {
        var value = get(index);
        if (value == null) return Optional.empty();
        if (!(value instanceof String string)) throw LuaValues.badArgumentOf(index, "string", value);
        return Optional.of(string);
    }

    /**
     * Get a string argument as a byte array.
     *
     * @param index The argument number.
     * @return The argument's value, or {@link Optional#empty()} if not present. This is a <em>read only</em> buffer.
     * @throws LuaException If the value is not a string.
     */
    default Optional<ByteBuffer> optBytes(int index) throws LuaException {
        return optString(index).map(LuaValues::encode);
    }

    /**
     * Get a string argument as an enum value.
     *
     * @param index The argument number.
     * @param klass The type of enum to parse.
     * @param <T>   The type of enum to parse.
     * @return The argument's value.
     * @throws LuaException If the value is not a string or not a valid option for this enum.
     */
    default <T extends Enum<T>> Optional<T> optEnum(int index, Class<T> klass) throws LuaException {
        var str = optString(index);
        return str.isPresent() ? Optional.of(LuaValues.checkEnum(index, klass, str.get())) : Optional.empty();
    }

    /**
     * Get an argument as a table.
     *
     * @param index The argument number.
     * @return The argument's value, or {@link Optional#empty()} if not present.
     * @throws LuaException If the value is not a table.
     */
    default Optional<Map<?, ?>> optTable(int index) throws LuaException {
        var value = get(index);
        if (value == null) return Optional.empty();
        if (!(value instanceof Map)) throw LuaValues.badArgumentOf(index, "map", value);
        return Optional.of((Map<?, ?>) value);
    }

    /**
     * Get an argument as a table in an unsafe manner.
     * <p>
     * Classes implementing this interface may choose to implement a more optimised version which does not copy the
     * table, instead returning a wrapper version, making it more efficient. However, the caller must guarantee that
     * they do not access off the computer thread (and so should not be used with main-thread functions) or once the
     * function call has finished (for instance, in callbacks).
     *
     * @param index The argument number.
     * @return The argument's value, or {@link Optional#empty()} if not present.
     * @throws LuaException If the value is not a table.
     */
    default Optional<LuaTable<?, ?>> optTableUnsafe(int index) throws LuaException {
        var value = get(index);
        if (value == null) return Optional.empty();
        if (!(value instanceof Map)) throw LuaValues.badArgumentOf(index, "map", value);
        return Optional.of(new ObjectLuaTable((Map<?, ?>) value));
    }

    /**
     * Get an argument as a double.
     *
     * @param index The argument number.
     * @param def   The default value, if this argument is not given.
     * @return The argument's value, or {@code def} if none was provided.
     * @throws LuaException If the value is not a number.
     */
    default double optDouble(int index, double def) throws LuaException {
        return optDouble(index).orElse(def);
    }

    /**
     * Get an argument as an int.
     *
     * @param index The argument number.
     * @param def   The default value, if this argument is not given.
     * @return The argument's value, or {@code def} if none was provided.
     * @throws LuaException If the value is not a number.
     */
    default int optInt(int index, int def) throws LuaException {
        return optInt(index).orElse(def);
    }

    /**
     * Get an argument as a long.
     *
     * @param index The argument number.
     * @param def   The default value, if this argument is not given.
     * @return The argument's value, or {@code def} if none was provided.
     * @throws LuaException If the value is not a number.
     */
    default long optLong(int index, long def) throws LuaException {
        return optLong(index).orElse(def);
    }

    /**
     * Get an argument as a finite number (not infinite or NaN).
     *
     * @param index The argument number.
     * @param def   The default value, if this argument is not given.
     * @return The argument's value, or {@code def} if none was provided.
     * @throws LuaException If the value is not finite.
     */
    default double optFiniteDouble(int index, double def) throws LuaException {
        return optFiniteDouble(index).orElse(def);
    }

    /**
     * Get an argument as a boolean.
     *
     * @param index The argument number.
     * @param def   The default value, if this argument is not given.
     * @return The argument's value, or {@code def} if none was provided.
     * @throws LuaException If the value is not a boolean.
     */
    default boolean optBoolean(int index, boolean def) throws LuaException {
        return optBoolean(index).orElse(def);
    }

    /**
     * Get an argument as a string.
     *
     * @param index The argument number.
     * @param def   The default value, if this argument is not given.
     * @return The argument's value, or {@code def} if none was provided.
     * @throws LuaException If the value is not a string.
     */
    @Nullable
    @Contract("_, !null -> !null")
    default String optString(int index, @Nullable String def) throws LuaException {
        return optString(index).orElse(def);
    }

    /**
     * Get an argument as a table.
     *
     * @param index The argument number.
     * @param def   The default value, if this argument is not given.
     * @return The argument's value, or {@code def} if none was provided.
     * @throws LuaException If the value is not a table.
     */
    @Nullable
    @Contract("_, !null -> !null")
    default Map<?, ?> optTable(int index, @Nullable Map<Object, Object> def) throws LuaException {
        return optTable(index).orElse(def);
    }
}
