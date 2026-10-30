// SPDX-FileCopyrightText: 2026 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package cc.tweaked.web.js.fetch;

import org.teavm.jso.JSBody;
import org.teavm.jso.JSObject;
import org.teavm.jso.JSProperty;
import org.teavm.jso.core.JSPromise;
import org.teavm.jso.file.Blob;
import org.teavm.jso.streams.ReadableStream;
import org.teavm.jso.typedarrays.ArrayBuffer;
import org.teavm.jso.typedarrays.Uint8Array;

/**
 * The {@link Response} interface of the Fetch API represents the response to a request.
 *
 * @see <a href="https://developer.mozilla.org/en-US/docs/Web/API/Response">MDN Reference</a>
 */
public abstract class Response implements JSObject {
    private Response() {
    }

    @JSBody(params = "url", script = "return fetch(url)")
    public static native JSPromise<Response> fetch(String url);

    @JSBody(params = { "url", "options" }, script = "return fetch(url, options)")
    public static native JSPromise<Response> fetch(String url, RequestInit options);

    @JSProperty
    public native int getStatus();

    @JSProperty
    public native String getStatusText();

    @JSProperty
    public native Headers getHeaders();

    @JSProperty
    public native ReadableStream getBody();

    public native JSPromise<ArrayBuffer> arrayBuffer();

    public native JSPromise<Blob> blob();

    public native JSPromise<Uint8Array> bytes();

    public native JSPromise<JSObject> json();

    public native JSPromise<String> text();
}
