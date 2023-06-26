// SPDX-FileCopyrightText: 2018 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.core.apis;

import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaValues;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.Optional;

import static dan200.computercraft.api.lua.LuaValues.getNumericType;

/**
 * Various helpers for tables.
 */
public final class TableHelper {
    private TableHelper() {
        throw new IllegalStateException("Cannot instantiate singleton " + getClass().getName());
    }

    public static LuaException badKey(String key, String expected, @Nullable Object actual) {
        return badKey(key, expected, LuaValues.getType(actual));
    }

    public static LuaException badKey(String key, String expected, String actual) {
        return new LuaException("bad field '" + key + "' (" + expected + " expected, got " + actual + ")");
    }

    public static double getNumberField(Map<?, ?> table, String key) throws LuaException {
        var value = table.get(key);
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        } else {
            throw badKey(key, "number", value);
        }
    }

    public static int getIntField(Map<?, ?> table, String key) throws LuaException {
        var value = table.get(key);
        if (value instanceof Number) {
            return (int) ((Number) value).longValue();
        } else {
            throw badKey(key, "number", value);
        }
    }

    public static double getRealField(Map<?, ?> table, String key) throws LuaException {
        return checkReal(key, getNumberField(table, key));
    }

    public static boolean getBooleanField(Map<?, ?> table, String key) throws LuaException {
        var value = table.get(key);
        if (value instanceof Boolean) {
            return (Boolean) value;
        } else {
            throw badKey(key, "boolean", value);
        }
    }

    public static String getStringField(Map<?, ?> table, String key) throws LuaException {
        var value = table.get(key);
        if (value instanceof String) {
            return (String) value;
        } else {
            throw badKey(key, "string", value);
        }
    }

    @SuppressWarnings("unchecked")
    public static Map<Object, Object> getTableField(Map<?, ?> table, String key) throws LuaException {
        var value = table.get(key);
        if (value instanceof Map) {
            return (Map<Object, Object>) value;
        } else {
            throw badKey(key, "table", value);
        }
    }

    public static double optNumberField(Map<?, ?> table, String key, double def) throws LuaException {
        var value = table.get(key);
        if (value == null) {
            return def;
        } else if (value instanceof Number) {
            return ((Number) value).doubleValue();
        } else {
            throw badKey(key, "number", value);
        }
    }

    public static int optIntField(Map<?, ?> table, String key, int def) throws LuaException {
        var value = table.get(key);
        if (value == null) {
            return def;
        } else if (value instanceof Number) {
            return (int) ((Number) value).longValue();
        } else {
            throw badKey(key, "number", value);
        }
    }

    public static Optional<Double> optRealField(Map<?, ?> table, String key) throws LuaException {
        var value = table.get(key);
        if (value == null) {
            return Optional.empty();
        } else {
            return Optional.of(getRealField(table, key));
        }
    }

    public static double optRealField(Map<?, ?> table, String key, double def) throws LuaException {
        return checkReal(key, optNumberField(table, key, def));
    }

    public static boolean optBooleanField(Map<?, ?> table, String key, boolean def) throws LuaException {
        var value = table.get(key);
        if (value == null) {
            return def;
        } else if (value instanceof Boolean) {
            return (Boolean) value;
        } else {
            throw badKey(key, "boolean", value);
        }
    }

    @Nullable
    public static String optStringField(Map<?, ?> table, String key, @Nullable String def) throws LuaException {
        var value = table.get(key);
        if (value == null) {
            return def;
        } else if (value instanceof String) {
            return (String) value;
        } else {
            throw badKey(key, "string", value);
        }
    }

    @SuppressWarnings("unchecked")
    public static Map<Object, Object> optTableField(Map<?, ?> table, String key, Map<Object, Object> def) throws LuaException {
        var value = table.get(key);
        if (value == null) {
            return def;
        } else if (value instanceof Map) {
            return (Map<Object, Object>) value;
        } else {
            throw badKey(key, "table", value);
        }
    }

    private static double checkReal(String key, double value) throws LuaException {
        if (!Double.isFinite(value)) throw badKey(key, "number", getNumericType(value));
        return value;
    }
}
