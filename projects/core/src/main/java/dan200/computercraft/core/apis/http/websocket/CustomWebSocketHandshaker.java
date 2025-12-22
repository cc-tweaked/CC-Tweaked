// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.core.apis.http.websocket;

import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshaker13;
import io.netty.handler.codec.http.websocketx.WebSocketVersion;
import org.jspecify.annotations.Nullable;

import java.net.URI;

/**
 * A version of {@link WebSocketClientHandshaker13} which retains the response headers, and doesn't add the
 * {@link HttpHeaderNames#ORIGIN} header to the original HTTP request.
 */
class CustomWebSocketHandshaker extends WebSocketClientHandshaker13 {
    private @Nullable HttpHeaders responseHeaders;

    CustomWebSocketHandshaker(URI webSocketURL, WebSocketVersion version, String subprotocol, boolean allowExtensions, HttpHeaders customHeaders, int maxFramePayloadLength) {
        super(webSocketURL, version, subprotocol, allowExtensions, customHeaders, maxFramePayloadLength);
    }

    @Override
    protected FullHttpRequest newHandshakeRequest() {
        var request = super.newHandshakeRequest();
        var headers = request.headers();
        if (!customHeaders.contains(HttpHeaderNames.ORIGIN)) headers.remove(HttpHeaderNames.ORIGIN);
        return request;
    }

    @Override
    protected void verify(FullHttpResponse response) {
        super.verify(response);
        responseHeaders = response.headers();
    }

    public @Nullable HttpHeaders getResponseHeaders() {
        return responseHeaders;
    }
}
