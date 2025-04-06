// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.core.apis.http.websocket;

import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshaker13;
import io.netty.handler.codec.http.websocketx.WebSocketVersion;

import java.net.URI;

/**
 * A version of {@link WebSocketClientHandshaker13} which doesn't add the {@link HttpHeaderNames#ORIGIN} header to the
 * original HTTP request.
 */
class NoOriginWebSocketHandshaker extends WebSocketClientHandshaker13 {
    NoOriginWebSocketHandshaker(URI webSocketURL, WebSocketVersion version, String subprotocol, boolean allowExtensions, HttpHeaders customHeaders, int maxFramePayloadLength) {
        super(webSocketURL, version, subprotocol, allowExtensions, customHeaders, maxFramePayloadLength);
    }

    @Override
    protected FullHttpRequest newHandshakeRequest() {
        var request = super.newHandshakeRequest();
        var headers = request.headers();
        if (!customHeaders.contains(HttpHeaderNames.ORIGIN)) headers.remove(HttpHeaderNames.ORIGIN);
        return request;
    }
}
