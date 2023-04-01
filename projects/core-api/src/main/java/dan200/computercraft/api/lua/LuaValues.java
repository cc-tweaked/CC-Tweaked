// SPDX-FileCopyrightText: 2020 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.api.lua;

import javax.annotation.Nullable;
import java.nio.ByteBuffer;
import java.util.Map;

/**
 * Various utility functions for operating with Lua values.
 *
 * @see IArguments
 */
public final class LuaValues {
    private LuaValues() {
    }

    /**
     * Encode a Lua string into a read-only {@link ByteBuffer}.
     *
     * @param string The string to encode.
     * @return The encoded string.
     */
    public static ByteBuffer encode(String string) {
        var chars = new byte[string.length()];
        for (var i = 0; i < chars.length; i++) {
            var c = string.charAt(i);
            chars[i] = c < 256 ? (byte) c : 63;
        }

        return ByteBuffer.wrap(chars).asReadOnlyBuffer();
    }

    /**
     * Returns a more detailed representation of this number's type. If this is finite, it will just return "number",
     * otherwise it returns whether it is infinite or NaN.
     *
     * @param value The value to extract the type for.
     * @return This value's numeric type.
     */
    public static String getNumericType(double value) {
        if (Double.isNaN(value)) return "nan";
        if (value == Double.POSITIVE_INFINITY) return "inf";
        if (value == Double.NEGATIVE_INFINITY) return "-inf";
        return "number";
    }

    /**
     * Get a string representation of the given value's type.
     *
     * @param value The value whose type we are trying to compute.
     * @return A string representation of the given value's type, in a similar format to that provided by Lua's
     * {@code type} function.
     */
    public static String getType(@Nullable Object value) {
        if (value == null) return "nil";
        if (value instanceof String) return "string";
        if (value instanceof Boolean) return "boolean";
        if (value instanceof Number) return "number";
        if (value instanceof Map) return "table";
        return "userdata";
    }

    /**
     * Construct a "bad argument" exception, from an {@link IArguments} argument and an expected type.
     *
     * @param arguments The current arguments.
     * @param index     The argument number, starting from 0.
     * @param expected  The expected type for this argument.
     * @return The constructed exception, which should be thrown immediately.
     */
    public static LuaException badArgumentOf(IArguments arguments, int index, String expected) {
        return badArgument(index, expected, arguments.getType(index));
    }

    /**
     * Construct a "bad argument" exception, from an expected and actual type.
     *
     * @param index    The argument number, starting from 0.
     * @param expected The expected type for this argument.
     * @param actual   The provided type for this argument.
     * @return The constructed exception, which should be thrown immediately.
     */
    public static LuaException badArgument(int index, String expected, String actual) {
        return new LuaException("bad argument #" + (index + 1) + " (" + expected + " expected, got " + actual + ")");
    }

    /**
     * Construct a table item exception, from an expected and actual type.
     *
     * @param index    The index into the table, starting from 1.
     * @param expected The expected type for this table item.
     * @param actual   The provided type for this table item.
     * @return The constructed exception, which should be thrown immediately.
     */
    public static LuaException badTableItem(int index, String expected, String actual) {
        return new LuaException("table item #" + index + " is not " + expected + " (got " + actual + ")");
    }

    /**
     * Construct a field exception, from an expected and actual type.
     *
     * @param key      The name of the field.
     * @param expected The expected type for this table item.
     * @param actual   The provided type for this table item.
     * @return The constructed exception, which should be thrown immediately.
     */
    public static LuaException badField(String key, String expected, String actual) {
        return new LuaException("field " + key + " is not " + expected + " (got " + actual + ")");
    }

    /**
     * Ensure a numeric argument is finite (i.e. not infinite or {@link Double#NaN}.
     *
     * @param index The argument index to check.
     * @param value The value to check.
     * @return The input {@code value}.
     * @throws LuaException If this is not a finite number.
     */
    public static Number checkFiniteNum(int index, Number value) throws LuaException {
        checkFinite(index, value.doubleValue());
        return value;
    }

    /**
     * Ensure a numeric argument is finite (i.e. not infinite or {@link Double#NaN}.
     *
     * @param index The argument index to check.
     * @param value The value to check.
     * @return The input {@code value}.
     * @throws LuaException If this is not a finite number.
     */
    public static double checkFinite(int index, double value) throws LuaException {
        if (!Double.isFinite(value)) throw badArgument(index, "number", getNumericType(value));
        return value;
    }

    /**
     * Ensure a string is a valid enum value.
     *
     * @param index The argument index to check.
     * @param klass The class of the enum instance.
     * @param value The value to extract.
     * @param <T>   The type of enum we are extracting.
     * @return The parsed enum value.
     * @throws LuaException If this is not a known enum value.
     */
    public static <T extends Enum<T>> T checkEnum(int index, Class<T> klass, String value) throws LuaException {
        for (var possibility : klass.getEnumConstants()) {
            if (possibility.name().equalsIgnoreCase(value)) return possibility;
        }

        throw new LuaException("bad argument #" + (index + 1) + " (unknown option " + value + ")");
    }
}
