// SPDX-FileCopyrightText: 2019 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.core.apis.http;

import com.google.common.base.Suppliers;
import dan200.computercraft.core.apis.http.options.Action;
import dan200.computercraft.core.apis.http.options.AddressRule;
import dan200.computercraft.core.apis.http.options.Options;
import dan200.computercraft.core.util.ThreadUtils;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ConnectTimeoutException;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.TooLongFrameException;
import io.netty.handler.codec.http.websocketx.CorruptedWebSocketFrameException;
import io.netty.handler.codec.http.websocketx.WebSocketCloseStatus;
import io.netty.handler.codec.http.websocketx.WebSocketHandshakeException;
import io.netty.handler.proxy.HttpProxyHandler;
import io.netty.handler.proxy.Socks4ProxyHandler;
import io.netty.handler.proxy.Socks5ProxyHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.timeout.ReadTimeoutException;
import io.netty.handler.traffic.AbstractTrafficShapingHandler;
import io.netty.handler.traffic.GlobalTrafficShapingHandler;
import org.jetbrains.annotations.VisibleForTesting;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLException;
import javax.net.ssl.SSLHandshakeException;
import java.net.Inet6Address;
import java.net.InetSocketAddress;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Just a shared object for executing simple HTTP related tasks.
 */
public final class NetworkUtils {
    private static final Logger LOG = LoggerFactory.getLogger(NetworkUtils.class);

    private static final EventLoopGroup LOOP_GROUP = new NioEventLoopGroup(4, ThreadUtils.lowPriorityFactory("Netty"));

    private final AbstractTrafficShapingHandler shapingHandler;

    private final NettyHttp netty;

    NetworkUtils(NettyHttp netty) {
        this.netty = netty;
        this.shapingHandler = new GlobalTrafficShapingHandler(
            LOOP_GROUP, netty.getConfig().uploadBandwidth(), netty.getConfig().downloadBandwidth()
        );
    }

    private static final Supplier<@Nullable SslContext> sslContext = Suppliers.memoize(() -> {
        try {
            return SslContextBuilder.forClient().build();
        } catch (SSLException e) {
            LOG.error("Cannot construct SSL context", e);
            return null;
        }
    });

    private static SslContext getSslContext() throws HTTPRequestException {
        var ssl = sslContext.get();
        if (ssl == null) throw new HTTPRequestException("Could not create a secure connection");
        return ssl;
    }

    void reloadConfig() {
        shapingHandler.configure(netty.getConfig().uploadBandwidth(), netty.getConfig().downloadBandwidth());
    }

    /**
     * Check that a URI is accessible, throwing an error if not.
     * <p>
     * Note, this may require a DNS lookup, and so should not be executed on the main CC thread.
     *
     * @param url The URL to check.
     * @throws HTTPRequestException If the URL is not permitted or is otherwise malformed.
     */
    public void checkUrl(HttpUri url) throws HTTPRequestException {
        getOptions(url.getHost(), getAddress(url.getHost(), url.getPort()));
    }

    public record ConnectionInfo(
        HttpUri uri,
        InetSocketAddress address,
        int timeout,
        Options options,
        @Nullable SslContext sslContext,
        @Nullable Consumer<SocketChannel> proxy
    ) {
    }

    /**
     * Check that a URI is accessible, throwing an error if not.
     * <p>
     * Note, this may require a DNS lookup, and so should not be executed on the main CC thread.
     *
     * @param uri     The URI to fetch.
     * @param timeout The timeout used for the connection.
     * @return The connection info.
     * @throws HTTPRequestException If the URL is not permitted or is otherwise malformed.
     * @see #connect(ConnectionInfo, Consumer)
     */
    public ConnectionInfo getConnectionInfo(HttpUri uri, int timeout) throws HTTPRequestException {
        var address = getAddress(uri.getHost(), uri.getPort());
        var options = getOptions(uri.getHost(), address);
        var sslContext = uri.getSsl() ? getSslContext() : null;
        var proxy = getProxyHandler(options, timeout);
        return new ConnectionInfo(uri, address, timeout, options, sslContext, proxy);
    }

    /**
     * Create a {@link InetSocketAddress} from the resolved {@code host} and port.
     * <p>
     * Note, this may require a DNS lookup, and so should not be executed on the main CC thread.
     *
     * @param host The host to resolve.
     * @param port The port, or -1 if not defined.
     * @return The resolved address.
     * @throws HTTPRequestException If the host is not malformed.
     */
    @VisibleForTesting
    static InetSocketAddress getAddress(String host, int port) throws HTTPRequestException {
        var socketAddress = new InetSocketAddress(host, port);
        if (socketAddress.isUnresolved()) throw new HTTPRequestException("Unknown host");
        if (socketAddress.getAddress() instanceof Inet6Address inet6 && (inet6.getScopedInterface() != null || inet6.getScopeId() != 0)) {
            throw new HTTPRequestException("Scoped address not permitted");
        }

        return socketAddress;
    }

    /**
     * Get options for a specific domain.
     *
     * @param host    The host to resolve.
     * @param address The address, resolved by {@link #getAddress(String, int)}.
     * @return The options for this host.
     * @throws HTTPRequestException If the host is not permitted
     */
    private Options getOptions(String host, InetSocketAddress address) throws HTTPRequestException {
        var options = AddressRule.apply(netty.getConfig().addressRules(), host, address);
        if (options.action() == Action.DENY) throw new HTTPRequestException("Domain not permitted");
        return options;
    }

    /**
     * Creates a proxy handler for a specific domain. Returns null if a proxy is not required for this HTTP rule, or
     * throws if it is required but is not configured correctly.
     * <p>
     * Note, this may require a DNS lookup, and so should not be executed on the main CC thread.
     *
     * @param options The options for the host to be proxied.
     * @param timeout The timeout for this connection. Currently only used for establishing the SSL initialisation.
     * @return A consumer that takes a {@link SocketChannel} and injects the proxy handler..
     * @throws HTTPRequestException If a proxy is required but not configured correctly.
     */
    private @Nullable Consumer<SocketChannel> getProxyHandler(Options options, int timeout) throws HTTPRequestException {
        if (!options.useProxy()) return null;

        var proxy = netty.getConfig().proxy();
        if (proxy == null) throw new HTTPRequestException("Proxy host not configured");

        var proxyAddress = new InetSocketAddress(proxy.host(), proxy.port());
        if (proxyAddress.isUnresolved()) throw new HTTPRequestException("Unknown proxy host");

        return switch (proxy.type()) {
            case HTTP ->
                ch -> ch.pipeline().addLast(new HttpProxyHandler(proxyAddress, proxy.username(), proxy.password()));
            case HTTPS -> {
                var sslContext = getSslContext();
                yield ch -> {
                    var p = ch.pipeline();
                    // If we're using an HTTPS proxy, we need to add an SSL handler for the proxy too.
                    p.addLast(makeSslHandler(ch, sslContext, timeout, proxy.host(), proxy.port()));
                    p.addLast(new HttpProxyHandler(proxyAddress, proxy.username(), proxy.password()));
                };
            }
            case SOCKS4 -> ch -> ch.pipeline().addLast(new Socks4ProxyHandler(proxyAddress, proxy.username()));
            case SOCKS5 ->
                ch -> ch.pipeline().addLast(new Socks5ProxyHandler(proxyAddress, proxy.username(), proxy.password()));
        };
    }

    /**
     * Connect to a socket using the supplied {@link ConnectionInfo} from {@link #getConnectionInfo(HttpUri, int)}.
     *
     * @param conn      The connection info.
     * @param configure A function to configure the channel.
     * @return The {@link ChannelFuture} from {@link Bootstrap#connect()}.
     */
    public ChannelFuture connect(ConnectionInfo conn, Consumer<SocketChannel> configure) {
        return new Bootstrap()
            .group(LOOP_GROUP)
            .channelFactory(NioSocketChannel::new)
            .handler(new ChannelInitializer<SocketChannel>() {
                @Override
                protected void initChannel(SocketChannel ch) {
                    setupChannel(ch, conn);
                    configure.accept(ch);
                }
            })
            .remoteAddress(conn.address())
            .connect();
    }

    /**
     * Make an SSL handler for the remote host.
     *
     * @param ch         The channel the handler will be added to.
     * @param sslContext The SSL context, if present.
     * @param timeout    The timeout on this channel.
     * @param peerHost   The host to connect to.
     * @param peerPort   The port to connect to.
     * @return The SSL handler.
     * @see io.netty.handler.ssl.SslHandler
     */
    private static SslHandler makeSslHandler(SocketChannel ch, SslContext sslContext, int timeout, String peerHost, int peerPort) {
        var handler = sslContext.newHandler(ch.alloc(), peerHost, peerPort);
        if (timeout > 0) handler.setHandshakeTimeoutMillis(timeout);
        return handler;
    }

    /**
     * Set up some basic properties of the channel. This adds a timeout, the traffic shaping handler, and the SSL
     * handler.
     *
     * @param ch   The channel to initialise.
     * @param conn Properties about the connection.
     * @see io.netty.channel.ChannelInitializer
     */
    private void setupChannel(SocketChannel ch, ConnectionInfo conn) {
        if (conn.timeout() > 0) ch.config().setConnectTimeoutMillis(conn.timeout());

        var p = ch.pipeline();
        p.addLast(shapingHandler);

        if (conn.proxy() != null) conn.proxy().accept(ch);

        if (conn.sslContext() != null) {
            p.addLast(makeSslHandler(ch, conn.sslContext(), conn.timeout(), conn.uri().getHost(), conn.address().getPort()));
        }
    }

    /**
     * Read a {@link ByteBuf} into a byte array.
     *
     * @param buffer The buffer to read.
     * @return The resulting bytes.
     */
    public static byte[] toBytes(ByteBuf buffer) {
        var bytes = new byte[buffer.readableBytes()];
        buffer.readBytes(bytes);
        return bytes;
    }

    public static String toFriendlyError(Throwable cause) {
        if (cause instanceof WebSocketHandshakeException || cause instanceof HTTPRequestException) {
            var message = cause.getMessage();
            return message == null ? "Could not connect" : message;
        } else if (cause instanceof TooLongFrameException) {
            return "Message is too large";
        } else if (cause instanceof ReadTimeoutException || cause instanceof ConnectTimeoutException) {
            return "Timed out";
        } else if (cause instanceof SSLHandshakeException || (cause instanceof DecoderException && cause.getCause() instanceof SSLHandshakeException)) {
            return "Could not create a secure connection";
        } else if (cause instanceof CorruptedWebSocketFrameException e) {
            return e.closeStatus() == WebSocketCloseStatus.MESSAGE_TOO_BIG
                ? "Received a too-large message"
                : "Corrupted websocket message";
        } else {
            return "Could not connect";
        }
    }
}
