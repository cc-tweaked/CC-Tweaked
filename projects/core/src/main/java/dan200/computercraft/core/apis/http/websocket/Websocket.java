// SPDX-FileCopyrightText: 2019 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.core.apis.http.websocket;

import com.google.common.base.Strings;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.core.Logging;
import dan200.computercraft.core.apis.IAPIEnvironment;
import dan200.computercraft.core.apis.http.HTTPRequestException;
import dan200.computercraft.core.apis.http.NetworkUtils;
import dan200.computercraft.core.apis.http.Resource;
import dan200.computercraft.core.apis.http.ResourceGroup;
import dan200.computercraft.core.apis.http.options.Options;
import dan200.computercraft.core.metrics.Metrics;
import dan200.computercraft.core.util.AtomicHelpers;
import dan200.computercraft.core.util.GlobalCleaner;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.websocketx.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Provides functionality to verify and connect to a remote websocket.
 */
public final class Websocket extends Resource<Websocket, Websocket.State> implements WebsocketClient {
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

    private final IAPIEnvironment environment;
    private final NetworkUtils network;
    private final String address;

    public Websocket(ResourceGroup<Websocket> limiter, IAPIEnvironment environment, NetworkUtils network, String address, URI uri, HttpHeaders headers, int timeout) {
        super(limiter, new Init(uri, headers, timeout));
        this.environment = environment;
        this.network = network;
        this.address = address;
    }

    /**
     * The base class for our websocket state machine.
     *
     * <pre>
     *   +-------------------+
     *   |   {@link Init}    |
     *   +---------+---------+
     *             | {@link #start()}
     *   +---------v----------+
     *   |  {@link Lookup}    +------------------------------------------>+
     *   +---------+----------+        Queue websocket_failure            |
     *             | {@link #connect(Init, NetworkUtils.ConnectionInfo)}  |
     *   +---------v----------+                                           |
     *   | {@link Connecting} +------------------------------------------>+
     *   +---------+----------+        Queue websocket_failure            |
     *             |                                                      |
     *             | Queue websocket_success                              |
     *             |                                                      |
     *   +---------v----------+                                           |
     *   |                    +------------------------------------------>+
     *   | {@link Connected}  |        Closed on server:                  |
     *   |                    |        Queue websocket_closed             |
     *   +---------+-------^--+                                           |
     *             |       | {@link #sendMessage(WebSocketFrame, long)}   |
     *             +-------+                                              |
     *             | {@link #close(int, String)}                          |
     *   +---------v----------+                                     +-----v----+
     *   |  {@link Closing}   +------------------------------------->  Closed  |
     *   +--------------------+        Closed on server or timeout: +----------+
     *                                 Queue: websocket_closed
     * </pre>
     */
    public sealed interface State extends ResourceState {
    }

    /**
     * The initial state, before a connection has been established.
     *
     * @param uri     The URL to connect to.
     * @param headers Headers for the connection.
     * @param timeout Timeout for the connection and handshake.
     */
    private record Init(URI uri, HttpHeaders headers, int timeout) implements State {
        @Override
        public void close() {
        }
    }

    /**
     * Currently looking up connection information.
     *
     * @param init   The initial state, containing properties to initialise the connection.
     * @param future The future used to look up the connection info.
     * @see #lookup(Init)
     */
    private record Lookup(Init init, Future<?> future) implements State {
        @Override
        public void close() {
            closeFuture(future);
        }
    }

    /**
     * Connecting to the remote server. This includes performing the remote handshake.
     *
     * @param channel The (possibly unresolved) channel.
     * @see #connect(Init, NetworkUtils.ConnectionInfo)
     */
    private record Connecting(ChannelFuture channel) implements State {
        @Override
        public void close() {
            closeChannel(channel);
        }
    }

    /**
     * A subset of {@link State}s where we have an open {@link WebsocketHandle}.
     */
    private sealed interface OpenWebsocketState extends State {
    }

    /**
     * A fully open and operational websocket state. We can {@linkplain #sendMessage(WebSocketFrame, long) send} and
     * receive messages in this state.
     *
     * @param channel  The currently open channel.
     * @param inFlight The number of in-flight websocket messages.
     * @param onSend   A channel listener used once messages have been sent.
     */
    private record Connected(
        Channel channel, AtomicInteger inFlight, ChannelFutureListener onSend
    ) implements OpenWebsocketState {
        private static Connected create(Channel channel) {
            var inFlight = new AtomicInteger(0);
            return new Connected(channel, inFlight, f -> inFlight.decrementAndGet());
        }

        @Override
        public void close() {
            closeChannel(channel);
        }
    }

    /**
     * A websocket which we have {@linkplain #close(int, String) started closing}, but not received the response from
     * the server. We can receive messages in this state, but not send new ones.
     *
     * @param channel    The currently open channel.
     * @param forceClose The task to force-close the socket if we do not receive a response in time.
     */
    private record Closing(Channel channel, Future<?> forceClose) implements OpenWebsocketState {
        @Override
        public void close() {
            closeChannel(channel);
            closeFuture(forceClose);
        }
    }

    @Override
    public void start() {
        transformStateAsync(Init.class, Lookup::new, this::lookup);
    }

    private StateTransition<State> lookup(Init init) {
        try {
            var uri = init.uri();
            var conn = network.getConnectionInfo(uri, uri.getScheme().equalsIgnoreCase("wss"), init.timeout());
            return () -> connect(init, conn);
        } catch (HTTPRequestException e) {
            return StateTransition.close(() -> queueHandshakeFailure(NetworkUtils.toFriendlyError(e)));
        } catch (Exception e) {
            LOG.error(Logging.HTTP_ERROR, "Error in websocket", e);
            return StateTransition.close(() -> queueHandshakeFailure(NetworkUtils.toFriendlyError(e)));
        }
    }

    private State connect(Init init, NetworkUtils.ConnectionInfo conn) {
        var options = conn.options();
        var channelFuture = network
            .connect(conn, ch -> {
                var headers = init.headers();
                var subprotocol = headers.get(HttpHeaderNames.SEC_WEBSOCKET_PROTOCOL);
                var handshaker = new CustomWebSocketHandshaker(
                    init.uri(), WebSocketVersion.V13, subprotocol, true, headers,
                    options.websocketMessage() <= 0 ? MAX_MESSAGE_SIZE : options.websocketMessage()
                );

                var p = ch.pipeline();
                p.addLast(
                    new HttpClientCodec(),
                    new HttpObjectAggregator(8192),
                    WebsocketCompressionHandler.INSTANCE,
                    new WebSocketClientProtocolHandler(handshaker, false, init.timeout()),
                    new WebsocketHandler(Websocket.this, handshaker, options)
                );
            })
            .addListener(c -> {
                if (!c.isSuccess()) handshakeFailure(NetworkUtils.toFriendlyError(c.cause()));
            });
        return new Connecting(channelFuture);
    }

    void success(HttpHeaders responseHeaders, Options options) {
        Map<String, String> headers = new HashMap<>();
        for (var header : responseHeaders) {
            headers.compute(header.getKey(), (k, existing) -> existing == null ? header.getValue() : existing + "," + header.getValue());
        }

        var handle = new WebsocketHandle(environment, address, this, headers, options);

        transformState(Connecting.class, connecting -> {
            GlobalCleaner.register(handle, () -> close(1001, ""));
            environment().queueEvent(SUCCESS_EVENT, address, handle);

            return Connected.create(connecting.channel().channel());
        });
    }

    private void queueHandshakeFailure(String message) {
        environment.queueEvent(FAILURE_EVENT, address, message);
    }

    void handshakeFailure(String message) {
        close(Connecting.class, () -> queueHandshakeFailure(message));
    }

    void serverClose(int status, String reason) {
        close(OpenWebsocketState.class, () -> environment.queueEvent(
            CLOSE_EVENT, address, Strings.isNullOrEmpty(reason) ? null : reason, status < 0 ? null : status
        ));
    }

    IAPIEnvironment environment() {
        return environment;
    }

    String address() {
        return address;
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
        try (var stateHolder = state()) {
            var state = stateHolder.get();

            // We cannot send a message on a closed or closing websocket.
            if (state == null || state instanceof Closing) throw new LuaException(CLOSED_ERROR);

            // It should be impossible to trigger this in an alternative state.
            if (!(state instanceof Connected connected)) throw new IllegalStateException("Unexpected state " + state);

            var channel = connected.channel;

            // Grow the number of in-flight requests, aborting if we've hit the limit. This is then decremented when the
            // promise finishes.
            if (!AtomicHelpers.incrementToLimit(connected.inFlight, ResourceGroup.DEFAULT_LIMIT)) {
                throw new LuaException("Too many ongoing websocket messages");
            }

            environment.observe(Metrics.WEBSOCKET_OUTGOING, size);
            channel.writeAndFlush(frame).addListener(connected.onSend);
        }
    }

    @Override
    public void close(int status, String reason) {
        transformState(state -> {
            // Close is idempotent, so should not fail if the socket is already closed.
            if (state == null || state instanceof Closing) return state;

            // It should be impossible to trigger this in another state but "Connected".
            if (!(state instanceof Connected connected)) throw new IllegalStateException("Unexpected state " + state);

            var channel = connected.channel();

            var packet = new CloseWebSocketFrame(status, reason);
            environment.observe(Metrics.WEBSOCKET_OUTGOING, packet.content().readableBytes());
            channel.writeAndFlush(packet);

            // Schedule a callback to force-close the websocket if the server does not respond. This reimplements
            // WebSocketProtocolHandler.applyCloseSentTimeout — unfortunately our CustomWebSocketHandshaker means we cannot
            // specify a custom forceCloseTimeoutMillis.
            var forceClose = channel.eventLoop().schedule(() -> serverClose(1006, ""), CLOSE_TIMEOUT, TimeUnit.SECONDS);
            return new Closing(connected.channel(), forceClose);
        });
    }
}
