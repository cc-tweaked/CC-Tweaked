// SPDX-FileCopyrightText: 2023 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package cc.tweaked.web.js;

import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.Nullable;
import org.teavm.jso.JSObject;
import org.teavm.jso.core.JSBoolean;
import org.teavm.jso.core.JSNumber;
import org.teavm.jso.core.JSString;

/**
 * Utility methods for converting between Java and Javascript representations.
 */
public class JavascriptConv {
    /**
     * Convert an array of Javascript values to an equivalent array of Java values.
     *
     * @param value The value to convert.
     * @return The converted value.
     */
    @Contract("null -> null; !null -> !null")
    public static @Nullable Object @Nullable [] toJava(@Nullable JSObject @Nullable [] value) {
        if (value == null) return null;
        var out = new Object[value.length];
        for (var i = 0; i < value.length; i++) out[i] = toJava(value[i]);
        return out;
    }

    /**
     * Convert a primitive Javascript value to a boxed Java object.
     *
     * @param value The value to convert.
     * @return The converted value.
     */
    public static @Nullable Object toJava(@Nullable JSObject value) {
        if (value == null) return null;
        if (value instanceof JSString v) return v.stringValue();
        if (value instanceof JSNumber v) return v.doubleValue();
        if (value instanceof JSBoolean v) return v.booleanValue();
        return null;
    }
}
