/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.core.apis.http.websocket;

import io.netty.channel.ChannelHandler;
import io.netty.handler.codec.compression.ZlibCodecFactory;
import io.netty.handler.codec.http.websocketx.extensions.WebSocketClientExtensionHandler;
import io.netty.handler.codec.http.websocketx.extensions.compression.DeflateFrameClientExtensionHandshaker;
import io.netty.handler.codec.http.websocketx.extensions.compression.PerMessageDeflateClientExtensionHandshaker;
import io.netty.handler.codec.http.websocketx.extensions.compression.WebSocketClientCompressionHandler;

import static io.netty.handler.codec.http.websocketx.extensions.compression.PerMessageDeflateServerExtensionHandshaker.MAX_WINDOW_SIZE;

/**
 * An alternative to {@link WebSocketClientCompressionHandler} which supports the {@code client_no_context_takeover}
 * extension. Makes CC <em>slightly</em> more flexible.
 */
@ChannelHandler.Sharable
final class WebsocketCompressionHandler extends WebSocketClientExtensionHandler
{
    public static final WebsocketCompressionHandler INSTANCE = new WebsocketCompressionHandler();

    private WebsocketCompressionHandler()
    {
        super(
            new PerMessageDeflateClientExtensionHandshaker(
                6, ZlibCodecFactory.isSupportingWindowSizeAndMemLevel(), MAX_WINDOW_SIZE,
                true, false
            ),
            new DeflateFrameClientExtensionHandshaker( false ),
            new DeflateFrameClientExtensionHandshaker( true )
        );

    }
}
