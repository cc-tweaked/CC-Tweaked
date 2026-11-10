// SPDX-FileCopyrightText: 2020 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.core.apis.http.options;

import com.google.errorprone.annotations.Immutable;

/**
 * Options for a given HTTP request or websocket, which control its resource constraints.
 *
 * @param action           Whether to {@link Action#ALLOW} or {@link Action#DENY} this request.
 * @param maxUpload        The maximum size of the HTTP request.
 * @param maxDownload      The maximum size of the HTTP response.
 * @param websocketMessage The maximum size of a websocket message (outgoing and incoming).
 * @param useProxy         Whether to use the configured proxy.
 */
@Immutable
public record Options(Action action, long maxUpload, long maxDownload, int websocketMessage, boolean useProxy) {
}
