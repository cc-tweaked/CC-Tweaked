// SPDX-FileCopyrightText: 2023 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package cc.tweaked.web.js;

import org.jetbrains.annotations.Contract;
import org.teavm.jso.JSBody;
import org.teavm.jso.JSByRef;
import org.teavm.jso.JSObject;
import org.teavm.jso.core.JSBoolean;
import org.teavm.jso.core.JSNumber;
import org.teavm.jso.core.JSObjects;
import org.teavm.jso.core.JSString;
import org.teavm.jso.typedarrays.ArrayBuffer;
import org.teavm.jso.typedarrays.Int8Array;

import javax.annotation.Nullable;
import java.nio.ByteBuffer;

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
    public static @Nullable Object[] toJava(@Nullable JSObject[] value) {
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
        return switch (JSObjects.typeOf(value)) {
            case "string" -> ((JSString) value).stringValue();
            case "number" -> ((JSNumber) value).doubleValue();
            case "boolean" -> ((JSBoolean) value).booleanValue();
            default -> null;
        };
    }

    /**
     * Check if an arbitrary object is a {@link ArrayBuffer}.
     *
     * @param object The object ot check
     * @return Whether this is an {@link ArrayBuffer}.
     */
    @JSBody(params = "data", script = "return data instanceof ArrayBuffer;")
    public static native boolean isArrayBuffer(JSObject object);

    /**
     * Wrap a JS {@link Int8Array} into a {@code byte[]}.
     *
     * @param view The array to wrap.
     * @return The wrapped array.
     */
    @JSByRef
    @JSBody(params = "x", script = "return x;")
    public static native byte[] asByteArray(Int8Array view);

    /**
     * Wrap a JS {@link ArrayBuffer} into a {@code byte[]}.
     *
     * @param view The array to wrap.
     * @return The wrapped array.
     */
    public static byte[] asByteArray(ArrayBuffer view) {
        return asByteArray(new Int8Array(view));
    }

    public static Int8Array toArray(ByteBuffer buffer) {
        var array = new Int8Array(buffer.remaining());
        for (var i = 0; i < array.getLength(); i++) array.set(i, buffer.get(i));
        return array;
    }
}
