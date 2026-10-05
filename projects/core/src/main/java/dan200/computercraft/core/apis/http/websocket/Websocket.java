// SPDX-FileCopyrightText: 2019 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.core.apis.http.websocket;

import com.google.common.base.Strings;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.core.Logging;
import dan200.computercraft.core.apis.IAPIEnvironment;
import dan200.computercraft.core.apis.http.*;
import dan200.computercraft.core.apis.http.options.Options;
import dan200.computercraft.core.metrics.Metrics;
import dan200.computercraft.core.util.AtomicHelpers;
import dan200.computercraft.core.util.GlobalCleaner;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.websocketx.*;
import io.netty.util.concurrent.GenericFutureListener;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Provides functionality to verify and connect to a remote websocket.
 */
public final class Websocket extends Resource<Websocket> implements WebsocketClient {
    private static final Logger LOG = LoggerFactory.getLogger(Websocket.class);

    /**
     * We declare the maximum size to be 2^30 bytes. While messages can be much longer, we set an arbitrary limit as
     * working with larger messages (especially within a Lua VM) is absurd.
     */
    public static final int MAX_MESSAGE_SIZE = 1 << 30;

    /**
     * The timeout after sending a {@link CloseWebSocketFrame} after which the channel should be force-closed.
     */
    private static final long CLOSE_TIMEOUT = 5;

    private @Nullable Future<?> executorFuture;
    private @Nullable ChannelFuture channelFuture;
    private @Nullable ScheduledFuture<?> closeFuture;

    private final IAPIEnvironment environment;
    private final NetworkUtils network;
    private final URI uri;
    private final String address;
    private final HttpHeaders headers;
    private final int timeout;

    private final AtomicInteger inFlight = new AtomicInteger(0);
    private final GenericFutureListener<? extends io.netty.util.concurrent.Future<? super Void>> onSend = f -> inFlight.decrementAndGet();
    private boolean isClosing = false;

    public Websocket(ResourceGroup<Websocket> limiter, IAPIEnvironment environment, NetworkUtils network, String address, URI uri, HttpHeaders headers, int timeout) {
        super(limiter);
        this.environment = environment;
        this.network = network;
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
            var conn = network.getConnectionInfo(uri, uri.getScheme().equalsIgnoreCase("wss"), timeout);
            var options = conn.options();

            // getConnectionInfo performs several blocking calls, so perform another cancellation check.
            if (isClosed()) return;

            channelFuture = network
                .connect(conn, ch -> {
                    var subprotocol = headers.get(HttpHeaderNames.SEC_WEBSOCKET_PROTOCOL);
                    var handshaker = new CustomWebSocketHandshaker(
                        uri, WebSocketVersion.V13, subprotocol, true, headers,
                        options.websocketMessage() <= 0 ? MAX_MESSAGE_SIZE : options.websocketMessage()
                    );

                    var p = ch.pipeline();
                    p.addLast(
                        new HttpClientCodec(),
                        new HttpObjectAggregator(8192),
                        WebsocketCompressionHandler.INSTANCE,
                        new WebSocketClientProtocolHandler(handshaker, false, timeout),
                        new WebsocketHandler(Websocket.this, handshaker, options)
                    );
                })
                .addListener(c -> {
                    if (!c.isSuccess()) handshakeFailure(NetworkUtils.toFriendlyError(c.cause()));
                });

            // Do an additional check for cancellation
            checkClosed();
        } catch (HTTPRequestException e) {
            handshakeFailure(NetworkUtils.toFriendlyError(e));
        } catch (Exception e) {
            handshakeFailure(NetworkUtils.toFriendlyError(e));
            LOG.error(Logging.HTTP_ERROR, "Error in websocket", e);
        }
    }

    void success(HttpHeaders responseHeaders, Options options) {
        if (isClosed()) return;

        Map<String, String> headers = new HashMap<>();
        for (var header : responseHeaders) {
            headers.compute(header.getKey(), (k, existing) -> existing == null ? header.getValue() : existing + "," + header.getValue());
        }


        var handle = new WebsocketHandle(environment, address, this, headers, options);
        GlobalCleaner.register(handle, () -> close(1001, ""));
        environment().queueEvent(SUCCESS_EVENT, address, handle);

        checkClosed();
    }

    void handshakeFailure(String message) {
        if (tryClose()) environment.queueEvent(FAILURE_EVENT, address, message);
    }

    void serverClose(int status, String reason) {
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
        closeFuture = closeFuture(closeFuture);
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
    public void sendText(String message) throws LuaException {
        sendMessage(new TextWebSocketFrame(message), message.length());
    }

    @Override
    public void sendBinary(ByteBuffer message) throws LuaException {
        long size = message.remaining();
        sendMessage(new BinaryWebSocketFrame(Unpooled.wrappedBuffer(message)), size);
    }

    private void sendMessage(WebSocketFrame frame, long size) throws LuaException {
        var channel = channel();
        if (channel == null || isClosing) throw new LuaException(CLOSED_ERROR);

        // Grow the number of in-flight requests, aborting if we've hit the limit. This is then decremented when the
        // promise finishes.
        if (!AtomicHelpers.incrementToLimit(inFlight, ResourceQueue.DEFAULT_LIMIT)) {
            throw new LuaException("Too many ongoing websocket messages");
        }

        environment.observe(Metrics.WEBSOCKET_OUTGOING, size);
        channel.writeAndFlush(frame).addListener(onSend);
    }

    @Override
    public void close(int status, String reason) {
        // Close is idempotent, so should not fail if the socket is already closed.
        var channel = channel();
        if (channel == null || isClosing) return;

        isClosing = true;

        var packet = new CloseWebSocketFrame(status, reason);
        environment.observe(Metrics.WEBSOCKET_OUTGOING, packet.content().readableBytes());
        channel.writeAndFlush(packet);

        // Schedule a callback to force-close the websocket if the server does not respond. This reimplements
        // WebSocketProtocolHandler.applyCloseSentTimeout — unfortunately our CustomWebSocketHandshaker means we cannot
        // specify a custom forceCloseTimeoutMillis.
        closeFuture = channel.eventLoop().schedule(() -> serverClose(1006, ""), CLOSE_TIMEOUT, TimeUnit.SECONDS);
    }
}
