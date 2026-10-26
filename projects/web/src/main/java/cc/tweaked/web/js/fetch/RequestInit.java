// SPDX-FileCopyrightText: 2026 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package cc.tweaked.web.js.fetch;

import cc.tweaked.web.js.abort.AbortSignal;
import org.teavm.jso.JSClass;
import org.teavm.jso.JSObject;
import org.teavm.jso.JSProperty;
import org.teavm.jso.core.JSMapLike;
import org.teavm.jso.core.JSObjects;
import org.teavm.jso.typedarrays.ArrayBuffer;
import org.teavm.jso.typedarrays.TypedArray;

/**
 * The properties that can be passed to {@link Response#fetch(String, RequestInit)}.
 *
 * @see <a href="https://developer.mozilla.org/en-US/docs/Web/API/RequestInit">MDN Reference</a>
 */
@JSClass(transparent = true)
public abstract class RequestInit implements JSObject {
    private RequestInit() {
    }

    public static RequestInit create() {
        return JSObjects.create();
    }

    @JSProperty
    public native void setMethod(String method);

    @JSProperty
    public native void setBody(String type);

    @JSProperty
    public native void setBody(ArrayBuffer body);

    @JSProperty
    public native void setBody(TypedArray body);

    @JSProperty
    public native void setHeaders(Headers headers);

    @JSProperty
    public native void setHeaders(JSMapLike<String> headers);

    @JSProperty
    public native void setSignal(AbortSignal signal);
}
