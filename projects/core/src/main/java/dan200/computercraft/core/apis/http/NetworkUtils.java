// SPDX-FileCopyrightText: 2019 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.core.apis.http;

import com.google.common.base.Strings;
import dan200.computercraft.core.CoreConfig;
import dan200.computercraft.core.apis.http.options.Action;
import dan200.computercraft.core.apis.http.options.AddressRule;
import dan200.computercraft.core.apis.http.options.Options;
import dan200.computercraft.core.util.ThreadUtils;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ConnectTimeoutException;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.TooLongFrameException;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLHandshakeException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Just a shared object for executing simple HTTP related tasks.
 */
public final class NetworkUtils {
    private static final Logger LOG = LoggerFactory.getLogger(NetworkUtils.class);

    public static final ScheduledThreadPoolExecutor EXECUTOR = new ScheduledThreadPoolExecutor(4, ThreadUtils.lowPriorityFactory("Network"));
    public static final EventLoopGroup LOOP_GROUP = new NioEventLoopGroup(4, ThreadUtils.lowPriorityFactory("Netty"));

    private static final AbstractTrafficShapingHandler SHAPING_HANDLER = new GlobalTrafficShapingHandler(
        EXECUTOR, CoreConfig.httpUploadBandwidth, CoreConfig.httpDownloadBandwidth
    );

    static {
        EXECUTOR.setKeepAliveTime(60, TimeUnit.SECONDS);
    }

    private NetworkUtils() {
    }

    private static final Object sslLock = new Object();
    private static @Nullable SslContext sslContext;
    private static boolean triedSslContext = false;

    private static @Nullable SslContext makeSslContext() {
        if (triedSslContext) return sslContext;
        synchronized (sslLock) {
            if (triedSslContext) return sslContext;

            triedSslContext = true;
            try {
                return sslContext = SslContextBuilder.forClient().build();
            } catch (SSLException e) {
                LOG.error("Cannot construct SSL context", e);
                return sslContext = null;
            }
        }
    }

    public static SslContext getSslContext() throws HTTPRequestException {
        var ssl = makeSslContext();
        if (ssl == null) throw new HTTPRequestException("Could not create a secure connection");
        return ssl;
    }

    public static void reloadConfig() {
        SHAPING_HANDLER.configure(CoreConfig.httpUploadBandwidth, CoreConfig.httpDownloadBandwidth);
    }

    public static void reset() {
        SHAPING_HANDLER.trafficCounter().resetCumulativeTime();
    }

    /**
     * Create a {@link InetSocketAddress} from a {@link java.net.URI}.
     * <p>
     * Note, this may require a DNS lookup, and so should not be executed on the main CC thread.
     *
     * @param uri The URI to fetch.
     * @param ssl Whether to connect with SSL. This is used to find the default port if not otherwise specified.
     * @return The resolved address.
     * @throws HTTPRequestException If the host is not malformed.
     */
    public static InetSocketAddress getAddress(URI uri, boolean ssl) throws HTTPRequestException {
        return getAddress(uri.getHost(), uri.getPort(), ssl);
    }

    /**
     * Create a {@link InetSocketAddress} from the resolved {@code host} and port.
     * <p>
     * Note, this may require a DNS lookup, and so should not be executed on the main CC thread.
     *
     * @param host The host to resolve.
     * @param port The port, or -1 if not defined.
     * @param ssl  Whether to connect with SSL. This is used to find the default port if not otherwise specified.
     * @return The resolved address.
     * @throws HTTPRequestException If the host is not malformed.
     */
    public static InetSocketAddress getAddress(String host, int port, boolean ssl) throws HTTPRequestException {
        if (port < 0) port = ssl ? 443 : 80;
        var socketAddress = new InetSocketAddress(host, port);
        if (socketAddress.isUnresolved()) throw new HTTPRequestException("Unknown host");
        return socketAddress;
    }

    /**
     * Get options for a specific domain.
     *
     * @param host    The host to resolve.
     * @param address The address, resolved by {@link #getAddress(String, int, boolean)}.
     * @return The options for this host.
     * @throws HTTPRequestException If the host is not permitted
     */
    public static Options getOptions(String host, InetSocketAddress address) throws HTTPRequestException {
        var options = AddressRule.apply(CoreConfig.httpRules, host, address);
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
    public static @Nullable Consumer<SocketChannel> getProxyHandler(Options options, int timeout) throws HTTPRequestException {
        if (!options.useProxy()) return null;

        var type = CoreConfig.httpProxyType;
        var host = CoreConfig.httpProxyHost;
        var port = CoreConfig.httpProxyPort;
        var username = CoreConfig.httpProxyUsername;
        var password = CoreConfig.httpProxyPassword;

        if (Strings.isNullOrEmpty(host)) {
            throw new HTTPRequestException("Proxy host not configured");
        }

        var proxyAddress = new InetSocketAddress(host, port);
        if (proxyAddress.isUnresolved()) throw new HTTPRequestException("Unknown proxy host");

        return switch (type) {
            case HTTP -> ch -> ch.pipeline().addLast(new HttpProxyHandler(proxyAddress, username, password));
            case HTTPS -> {
                var sslContext = getSslContext();
                yield ch -> {
                    var p = ch.pipeline();
                    // If we're using an HTTPS proxy, we need to add an SSL handler for the proxy too.
                    p.addLast(makeSslHandler(ch, sslContext, timeout, host, port));
                    p.addLast(new HttpProxyHandler(proxyAddress, username, password));
                };
            }
            case SOCKS4 -> ch -> ch.pipeline().addLast(new Socks4ProxyHandler(proxyAddress, username));
            case SOCKS5 -> ch -> ch.pipeline().addLast(new Socks5ProxyHandler(proxyAddress, username, password));
        };
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
     * @param ch            The channel to initialise.
     * @param uri           The URI to connect to.
     * @param socketAddress The address of the socket to connect to.
     * @param sslContext    The SSL context, if present.
     * @param proxy         The proxy handler, if present.
     * @param timeout       The timeout on this channel.
     * @see io.netty.channel.ChannelInitializer
     */
    public static void initChannel(SocketChannel ch, URI uri, InetSocketAddress socketAddress, @Nullable SslContext sslContext, @Nullable Consumer<SocketChannel> proxy, int timeout) {
        if (timeout > 0) ch.config().setConnectTimeoutMillis(timeout);

        var p = ch.pipeline();
        p.addLast(SHAPING_HANDLER);

        if (proxy != null) proxy.accept(ch);

        if (sslContext != null) {
            p.addLast(makeSslHandler(ch, sslContext, timeout, uri.getHost(), socketAddress.getPort()));
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
        } else {
            return "Could not connect";
        }
    }
}
