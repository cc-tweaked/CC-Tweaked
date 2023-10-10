// SPDX-FileCopyrightText: 2019 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.core.apis.http.websocket;

import com.google.common.base.Strings;
import dan200.computercraft.core.Logging;
import dan200.computercraft.core.apis.IAPIEnvironment;
import dan200.computercraft.core.apis.http.HTTPRequestException;
import dan200.computercraft.core.apis.http.NetworkUtils;
import dan200.computercraft.core.apis.http.Resource;
import dan200.computercraft.core.apis.http.ResourceGroup;
import dan200.computercraft.core.apis.http.options.Options;
import dan200.computercraft.core.metrics.Metrics;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketClientProtocolHandler;
import io.netty.handler.codec.http.websocketx.WebSocketVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.concurrent.Future;

/**
 * Provides functionality to verify and connect to a remote websocket.
 */
public class Websocket extends Resource<Websocket> implements WebsocketClient {
    private static final Logger LOG = LoggerFactory.getLogger(Websocket.class);

    /**
     * We declare the maximum size to be 2^30 bytes. While messages can be much longer, we set an arbitrary limit as
     * working with larger messages (especially within a Lua VM) is absurd.
     */
    public static final int MAX_MESSAGE_SIZE = 1 << 30;

    private @Nullable Future<?> executorFuture;
    private @Nullable ChannelFuture channelFuture;

    private final IAPIEnvironment environment;
    private final URI uri;
    private final String address;
    private final HttpHeaders headers;
    private final int timeout;

    public Websocket(ResourceGroup<Websocket> limiter, IAPIEnvironment environment, URI uri, String address, HttpHeaders headers, int timeout) {
        super(limiter);
        this.environment = environment;
        this.uri = uri;
        this.address = address;
        this.headers = headers;
        this.timeout = timeout;
    }

    public void connect() {
        if (isClosed()) return;
        executorFuture = NetworkUtils.EXECUTOR.submit(this::doConnect);
        checkClosed();
    }

    private void doConnect() {
        // If we're cancelled, abort.
        if (isClosed()) return;

        try {
            var ssl = uri.getScheme().equalsIgnoreCase("wss");
            var socketAddress = NetworkUtils.getAddress(uri, ssl);
            var options = NetworkUtils.getOptions(uri.getHost(), socketAddress);
            var sslContext = ssl ? NetworkUtils.getSslContext() : null;
            var proxy = NetworkUtils.getProxyHandler(options, timeout);

            // getAddress may have a slight delay, so let's perform another cancellation check.
            if (isClosed()) return;

            channelFuture = new Bootstrap()
                .group(NetworkUtils.LOOP_GROUP)
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        NetworkUtils.initChannel(ch, uri, socketAddress, sslContext, proxy, timeout);

                        var subprotocol = headers.get(HttpHeaderNames.SEC_WEBSOCKET_PROTOCOL);
                        var handshaker = new NoOriginWebSocketHandshaker(
                            uri, WebSocketVersion.V13, subprotocol, true, headers,
                            options.websocketMessage() <= 0 ? MAX_MESSAGE_SIZE : options.websocketMessage()
                        );

                        var p = ch.pipeline();
                        p.addLast(
                            new HttpClientCodec(),
                            new HttpObjectAggregator(8192),
                            WebsocketCompressionHandler.INSTANCE,
                            new WebSocketClientProtocolHandler(handshaker, false, timeout),
                            new WebsocketHandler(Websocket.this, options)
                        );
                    }
                })
                .remoteAddress(socketAddress)
                .connect()
                .addListener(c -> {
                    if (!c.isSuccess()) failure(NetworkUtils.toFriendlyError(c.cause()));
                });

            // Do an additional check for cancellation
            checkClosed();
        } catch (HTTPRequestException e) {
            failure(NetworkUtils.toFriendlyError(e));
        } catch (Exception e) {
            failure(NetworkUtils.toFriendlyError(e));
            LOG.error(Logging.HTTP_ERROR, "Error in websocket", e);
        }
    }

    void success(Options options) {
        if (isClosed()) return;

        var handle = new WebsocketHandle(environment, address, this, options);
        environment().queueEvent(SUCCESS_EVENT, address, handle);
        createOwnerReference(handle);

        checkClosed();
    }

    void failure(String message) {
        if (tryClose()) environment.queueEvent(FAILURE_EVENT, address, message);
    }

    void close(int status, String reason) {
        if (tryClose()) {
            environment.queueEvent(CLOSE_EVENT, address,
                Strings.isNullOrEmpty(reason) ? null : reason,
                status < 0 ? null : status);
        }
    }

    @Override
    protected void dispose() {
        super.dispose();

        executorFuture = closeFuture(executorFuture);
        channelFuture = closeChannel(channelFuture);
    }

    IAPIEnvironment environment() {
        return environment;
    }

    String address() {
        return address;
    }

    private @Nullable Channel channel() {
        var channel = channelFuture;
        return channel == null ? null : channel.channel();
    }

    @Override
    public void sendText(String message) {
        environment.observe(Metrics.WEBSOCKET_OUTGOING, message.length());

        var channel = channel();
        if (channel != null) channel.writeAndFlush(new TextWebSocketFrame(message));
    }

    @Override
    public void sendBinary(ByteBuffer message) {
        environment.observe(Metrics.WEBSOCKET_OUTGOING, message.remaining());

        var channel = channel();
        if (channel != null) channel.writeAndFlush(new BinaryWebSocketFrame(Unpooled.wrappedBuffer(message)));
    }
}
