// SPDX-FileCopyrightText: 2019 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.core.apis.http.websocket;

import dan200.computercraft.core.apis.http.NetworkUtils;
import dan200.computercraft.core.apis.http.options.Options;
import dan200.computercraft.core.metrics.Metrics;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.websocketx.*;
import io.netty.util.CharsetUtil;

import static dan200.computercraft.core.apis.http.websocket.WebsocketClient.MESSAGE_EVENT;

class WebsocketHandler extends SimpleChannelInboundHandler<Object> {
    private final Websocket websocket;
    private final Options options;
    private boolean handshakeComplete = false;

    WebsocketHandler(Websocket websocket, Options options) {
        this.websocket = websocket;
        this.options = options;
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        fail("Connection closed");
        super.channelInactive(ctx);
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) {
        if (evt == WebSocketClientProtocolHandler.ClientHandshakeStateEvent.HANDSHAKE_COMPLETE) {
            websocket.success(options);
            handshakeComplete = true;
        } else if (evt == WebSocketClientProtocolHandler.ClientHandshakeStateEvent.HANDSHAKE_TIMEOUT) {
            websocket.failure("Timed out");
        }
    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, Object msg) {
        if (websocket.isClosed()) return;

        if (msg instanceof FullHttpResponse response) {
            throw new IllegalStateException("Unexpected FullHttpResponse (getStatus=" + response.status() + ", content=" + response.content().toString(CharsetUtil.UTF_8) + ')');
        }

        var frame = (WebSocketFrame) msg;
        if (frame instanceof TextWebSocketFrame textFrame) {
            var data = NetworkUtils.toBytes(textFrame.content());

            websocket.environment().observe(Metrics.WEBSOCKET_INCOMING, data.length);
            websocket.environment().queueEvent(MESSAGE_EVENT, websocket.address(), data, false);
        } else if (frame instanceof BinaryWebSocketFrame) {
            var data = NetworkUtils.toBytes(frame.content());

            websocket.environment().observe(Metrics.WEBSOCKET_INCOMING, data.length);
            websocket.environment().queueEvent(MESSAGE_EVENT, websocket.address(), data, true);
        } else if (frame instanceof CloseWebSocketFrame closeFrame) {
            websocket.close(closeFrame.statusCode(), closeFrame.reasonText());
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        ctx.close();

        fail(NetworkUtils.toFriendlyError(cause));
    }

    private void fail(String message) {
        if (handshakeComplete) {
            websocket.close(-1, message);
        } else {
            websocket.failure(message);
        }
    }
}
