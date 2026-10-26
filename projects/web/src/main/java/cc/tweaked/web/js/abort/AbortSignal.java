// SPDX-FileCopyrightText: 2026 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package cc.tweaked.web.js.abort;

import org.teavm.jso.JSObject;
import org.teavm.jso.JSProperty;
import org.teavm.jso.dom.events.EventTarget;

/**
 * The {@link AbortSignal} interface represents a signal object that allows you to communicate with an asynchronous
 * operation (such as a fetch request) and abort it if required via an {@link AbortController} object.
 *
 * @see <a href="https://developer.mozilla.org/en-US/docs/Web/API/AbortSignal">MDN Reference</a>
 */
public interface AbortSignal extends EventTarget {
    @JSProperty
    boolean getAborted();

    @JSProperty
    JSObject getReason();
}
