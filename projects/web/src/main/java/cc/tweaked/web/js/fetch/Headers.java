// SPDX-FileCopyrightText: 2026 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package cc.tweaked.web.js.fetch;

import org.teavm.jso.JSClass;
import org.teavm.jso.JSFunctor;
import org.teavm.jso.JSObject;

/**
 * The {@link Headers} interface of the Fetch API allows you to perform various actions on HTTP request and response
 * headers. These actions include retrieving, setting, adding to, and removing headers from the list of the request's
 * headers.
 *
 * @see <a href="https://developer.mozilla.org/en-US/docs/Web/API/Headers">MDN Reference</a>
 */
@JSClass
public class Headers implements JSObject {
    public native void append(String key, String value);

    public native void forEach(ForEachConsumer consumer);

    @JSFunctor
    @FunctionalInterface
    public interface ForEachConsumer extends JSObject {
        void accept(String value, String key, Headers object);
    }
}
