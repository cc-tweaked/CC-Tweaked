// SPDX-FileCopyrightText: 2023 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.core.apis.http

import io.netty.bootstrap.ServerBootstrap
import io.netty.buffer.ByteBufUtil
import io.netty.buffer.Unpooled
import io.netty.channel.*
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.handler.codec.http.*
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame
import io.netty.handler.codec.http.websocketx.WebSocketFrame
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler.HandshakeComplete
import io.netty.handler.codec.http.websocketx.extensions.compression.WebSocketServerCompressionHandler
import java.net.InetSocketAddress
import java.nio.charset.StandardCharsets

class HttpServer(val port: Int, private val workerGroup: EventLoopGroup, private val activeConnections: Set<Channel>) {
    /** Stop the server from running */
    fun stop() {
        workerGroup.shutdownGracefully()
    }

    /** Broadcast this message to every connected websocket */
    fun broadcast(message: WebSocketFrame) {
        for (chan in activeConnections) chan.writeAndFlush(message)
    }

    companion object {
        /** Runs a small HTTP server to run alongside [TestHttpApi] */
        fun runServer(run: (server: HttpServer) -> Unit) {
            val workerGroup: EventLoopGroup = NioEventLoopGroup(2)
            val activeConnections = mutableSetOf<Channel>()
            try {
                val ch = ServerBootstrap()
                    .group(workerGroup)
                    .channel(NioServerSocketChannel::class.java)
                    .childHandler(
                        object : ChannelInitializer<SocketChannel>() {
                            override fun initChannel(ch: SocketChannel) {
                                val p: ChannelPipeline = ch.pipeline()
                                p.addLast(HttpServerCodec())
                                p.addLast(HttpContentCompressor())
                                p.addLast(HttpObjectAggregator(8192))
                                p.addLast(HttpServerHandler())
                                p.addLast(WebSocketServerCompressionHandler())
                                p.addLast(WebSocketServerProtocolHandler("/ws", null, true))
                                p.addLast(WebSocketFrameHandler(activeConnections))
                            }
                        },
                    ).bind(0).sync().channel()
                val port = (ch.localAddress() as InetSocketAddress).port
                try {
                    run(HttpServer(port, workerGroup, activeConnections))
                } finally {
                    ch.close().sync()
                }
            } finally {
                workerGroup.shutdownGracefully().get()
            }
        }
    }
}

/**
 * A HTTP handler which hosts `/` (a simple static page) and `/ws` (see [WebSocketFrameHandler])
 */
private class HttpServerHandler : SimpleChannelInboundHandler<FullHttpRequest>() {
    companion object {
        private val CONTENT = "Hello, world!".toByteArray(StandardCharsets.UTF_8)
    }

    override fun channelReadComplete(ctx: ChannelHandlerContext) {
        ctx.flush()
    }

    override fun channelRead0(ctx: ChannelHandlerContext, request: FullHttpRequest) {
        when (request.uri()) {
            "/", "/index.html" -> handleIndex(ctx, request)
            "/ws" -> handleWebsocket(ctx, request)
            else -> sendHttpResponse(ctx, request, DefaultFullHttpResponse(request.protocolVersion(), HttpResponseStatus.NOT_FOUND))
        }
    }

    private fun handleIndex(ctx: ChannelHandlerContext, request: FullHttpRequest) {
        sendHttpResponse(
            ctx,
            request,
            DefaultFullHttpResponse(request.protocolVersion(), HttpResponseStatus.OK, Unpooled.wrappedBuffer(CONTENT)),
        )
    }

    private fun handleWebsocket(ctx: ChannelHandlerContext, request: FullHttpRequest) {
        if (!request.headers().contains(HttpHeaderNames.UPGRADE, HttpHeaderValues.WEBSOCKET, true)) {
            return sendHttpResponse(ctx, request, DefaultFullHttpResponse(request.protocolVersion(), HttpResponseStatus.BAD_REQUEST))
        }

        ctx.fireChannelRead(request.retain())
    }

    private fun sendHttpResponse(ctx: ChannelHandlerContext, request: FullHttpRequest, response: FullHttpResponse) {
        // Generate an error page if response getStatus code is not OK (200).
        val responseStatus = response.status()
        if (responseStatus.code() != 200) {
            ByteBufUtil.writeUtf8(response.content(), responseStatus.toString())
            HttpUtil.setContentLength(response, response.content().readableBytes().toLong())
        }

        // Send the response and close the connection if necessary.
        val keepAlive = HttpUtil.isKeepAlive(request) && responseStatus.code() == 200
        HttpUtil.setKeepAlive(response, keepAlive)
        val future = ctx.writeAndFlush(response)
        if (!keepAlive) future.addListener(ChannelFutureListener.CLOSE)
    }
}

/**
 * A basic WS server which just sends back the original message.
 */
private class WebSocketFrameHandler(private val activeConnections: MutableSet<Channel>) : SimpleChannelInboundHandler<WebSocketFrame>() {
    override fun channelRead0(ctx: ChannelHandlerContext, frame: WebSocketFrame) {
        if (frame is TextWebSocketFrame) {
            // Send the uppercase string back.
            val request = frame.text()
            ctx.channel().writeAndFlush(TextWebSocketFrame(request.uppercase()))
        } else {
            throw UnsupportedOperationException("unsupported frame type: ${frame.javaClass.name}")
        }
    }

    override fun userEventTriggered(ctx: ChannelHandlerContext, evt: Any) {
        if (evt is HandshakeComplete) {
            // Channel upgrade to websocket, remove HttpServerHandler.
            ctx.pipeline().remove(HttpServerHandler::class.java)
            activeConnections.add(ctx.channel())
        } else {
            super.userEventTriggered(ctx, evt)
        }
    }

    override fun channelInactive(ctx: ChannelHandlerContext) {
        super.channelInactive(ctx)
        activeConnections.remove(ctx.channel())
    }
}
