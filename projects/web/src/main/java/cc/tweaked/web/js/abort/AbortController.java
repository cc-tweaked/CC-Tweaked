// SPDX-FileCopyrightText: 2026 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package cc.tweaked.web.js.abort;

import org.teavm.jso.JSClass;
import org.teavm.jso.JSObject;
import org.teavm.jso.JSProperty;

/**
 * The {@link AbortController} interface represents a controller object that allows you to abort one or more Web
 * requests as and when desired.
 *
 * @see <a href="https://developer.mozilla.org/en-US/docs/Web/API/AbortController">MDN Reference</a>
 */
@JSClass
public class AbortController implements JSObject {
    public AbortController() {
    }

    @JSProperty
    public native AbortSignal getSignal();

    public native void abort();

    public native void abort(String reason);

    public native void abort(JSObject reason);
}
