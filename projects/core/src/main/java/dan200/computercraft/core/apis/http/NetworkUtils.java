// SPDX-FileCopyrightText: 2019 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.core.apis.http;

import dan200.computercraft.core.CoreConfig;
import dan200.computercraft.core.apis.http.options.Action;
import dan200.computercraft.core.apis.http.options.AddressRule;
import dan200.computercraft.core.apis.http.options.Options;
import dan200.computercraft.core.util.ThreadUtils;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ConnectTimeoutException;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.TooLongFrameException;
import io.netty.handler.codec.http.websocketx.WebSocketHandshakeException;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.timeout.ReadTimeoutException;
import io.netty.handler.traffic.AbstractTrafficShapingHandler;
import io.netty.handler.traffic.GlobalTrafficShapingHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.TrustManagerFactory;
import java.net.InetSocketAddress;
import java.net.URI;
import java.security.KeyStore;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Just a shared object for executing simple HTTP related tasks.
 */
public final class NetworkUtils {
    private static final Logger LOG = LoggerFactory.getLogger(NetworkUtils.class);

    public static final ScheduledThreadPoolExecutor EXECUTOR = new ScheduledThreadPoolExecutor(
        4,
        ThreadUtils.builder("Network")
            .setPriority(Thread.MIN_PRIORITY + (Thread.NORM_PRIORITY - Thread.MIN_PRIORITY) / 2)
            .build()
    );

    public static final EventLoopGroup LOOP_GROUP = new NioEventLoopGroup(4, ThreadUtils.builder("Netty")
        .setPriority(Thread.MIN_PRIORITY + (Thread.NORM_PRIORITY - Thread.MIN_PRIORITY) / 2)
        .build()
    );

    public static final AbstractTrafficShapingHandler SHAPING_HANDLER = new GlobalTrafficShapingHandler(
        EXECUTOR, CoreConfig.httpUploadBandwidth, CoreConfig.httpDownloadBandwidth
    );

    static {
        EXECUTOR.setKeepAliveTime(60, TimeUnit.SECONDS);
    }

    private NetworkUtils() {
    }

    private static final Object sslLock = new Object();
    private static @Nullable TrustManagerFactory trustManager;
    private static @Nullable SslContext sslContext;
    private static boolean triedSslContext = false;

    @Nullable
    private static TrustManagerFactory getTrustManager() {
        if (trustManager != null) return trustManager;
        synchronized (sslLock) {
            if (trustManager != null) return trustManager;

            TrustManagerFactory tmf = null;
            try {
                tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
                tmf.init((KeyStore) null);
            } catch (Exception e) {
                LOG.error("Cannot setup trust manager", e);
            }

            return trustManager = tmf;
        }
    }

    @Nullable
    public static SslContext getSslContext() throws HTTPRequestException {
        if (sslContext != null || triedSslContext) return sslContext;
        synchronized (sslLock) {
            if (sslContext != null || triedSslContext) return sslContext;
            try {
                return sslContext = SslContextBuilder
                    .forClient()
                    .trustManager(getTrustManager())
                    .build();
            } catch (SSLException e) {
                LOG.error("Cannot construct SSL context", e);
                triedSslContext = true;
                sslContext = null;

                throw new HTTPRequestException("Cannot create a secure connection");
            }
        }
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
        if (options.action == Action.DENY) throw new HTTPRequestException("Domain not permitted");
        return options;
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
