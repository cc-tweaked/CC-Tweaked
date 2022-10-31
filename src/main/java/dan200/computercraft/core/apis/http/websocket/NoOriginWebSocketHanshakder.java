/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
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
public class NoOriginWebSocketHanshakder extends WebSocketClientHandshaker13
{
    public NoOriginWebSocketHanshakder( URI webSocketURL, WebSocketVersion version, String subprotocol, boolean allowExtensions, HttpHeaders customHeaders, int maxFramePayloadLength )
    {
        super( webSocketURL, version, subprotocol, allowExtensions, customHeaders, maxFramePayloadLength );
    }

    @Override
    protected FullHttpRequest newHandshakeRequest()
    {
        FullHttpRequest request = super.newHandshakeRequest();
        HttpHeaders headers = request.headers();
        if( !customHeaders.contains( HttpHeaderNames.ORIGIN ) ) headers.remove( HttpHeaderNames.ORIGIN );
        return request;
    }
}
