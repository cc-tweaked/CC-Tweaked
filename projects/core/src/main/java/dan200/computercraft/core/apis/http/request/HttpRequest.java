// SPDX-FileCopyrightText: 2019 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.core.apis.http.request;

import dan200.computercraft.core.Logging;
import dan200.computercraft.core.apis.IAPIEnvironment;
import dan200.computercraft.core.apis.http.*;
import dan200.computercraft.core.metrics.Metrics;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.handler.codec.http.*;
import io.netty.handler.timeout.ReadTimeoutHandler;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.nio.ByteBuffer;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Represents an in-progress HTTP request.
 */
public final class HttpRequest extends Resource<HttpRequest, HttpRequest.State> {
    private static final Logger LOG = LoggerFactory.getLogger(HttpRequest.class);
    private static final String SUCCESS_EVENT = "http_success";
    private static final String FAILURE_EVENT = "http_failure";

    private static final int MAX_REDIRECTS = 16;

    private final IAPIEnvironment environment;
    private final NetworkUtils network;

    private final String address;
    private final ByteBuf postBuffer;
    private final HttpHeaders headers;
    private final boolean binary;
    private final int timeout;

    final AtomicInteger redirects;

    public HttpRequest(
        ResourceGroup<HttpRequest> limiter, IAPIEnvironment environment, NetworkUtils network,
        String address, HttpUri uri, HttpMethod method, @Nullable ByteBuffer postBody, HttpHeaders headers,
        boolean binary, boolean followRedirects, int timeout
    ) {
        super(limiter, new Init(uri, method));
        this.environment = environment;
        this.network = network;
        this.address = address;
        postBuffer = postBody != null
            ? Unpooled.wrappedBuffer(postBody)
            : Unpooled.buffer(0);
        this.headers = headers;
        this.binary = binary;
        redirects = new AtomicInteger(followRedirects ? MAX_REDIRECTS : 0);
        this.timeout = timeout;

        if (postBody != null) {
            if (!headers.contains(HttpHeaderNames.CONTENT_TYPE)) {
                headers.set(HttpHeaderNames.CONTENT_TYPE, "application/x-www-form-urlencoded; charset=utf-8");
            }

            if (!headers.contains(HttpHeaderNames.CONTENT_LENGTH)) {
                headers.set(HttpHeaderNames.CONTENT_LENGTH, postBuffer.readableBytes());
            }
        }
    }

    public IAPIEnvironment environment() {
        return environment;
    }

    public static HttpUri checkUri(String address) throws HTTPRequestException {
        return HttpUri.check(address, "http", "https", null);
    }

    private static HttpUri checkUri(URI url) throws HTTPRequestException {
        return HttpUri.check(url, "http", "https");
    }

    /**
     * The base class for our request state machine.
     *
     * <pre>
     *   +-------------------+
     *   |   {@link Init}    |
     *   +---------+---------+
     *             | {@link #start()}/{@link #lookup(HttpUri, HttpMethod)}
     *   +---------v----------+
     *   |  {@link Lookup}    +------------------------------------------>+
     *   +-^-------+----------+        Queue http_failure                 |
     *     |       | {@link #connect(HttpUri, HttpMethod, NetworkUtils.ConnectionInfo, long)}
     *   +-+-------v----------+                                           |
     *   | {@link Connecting} +------------------------------------------>+
     *   +---------+----------+        Queue http_failure                 |
     *             |                                                      |
     *             | Queue http_success                                   |
     *             |                                                      |
     *             |                                                +-----v----+
     *             +------------------------------------------------>  Closed  |
     *                                                              +----------+
     * </pre>
     */
    public sealed interface State extends ResourceState {
    }

    /**
     * The initial state, before a connection has been established.
     *
     * @param uri    The URL to connect to.
     * @param method The HTTP method to connect with.
     */
    private record Init(HttpUri uri, HttpMethod method) implements State {
        @Override
        public void close() {
        }
    }

    /**
     * Currently looking up connection information.
     *
     * @param future The future used to look up the connection info.
     * @see #lookup(HttpUri, HttpMethod)
     */
    private record Lookup(Future<?> future) implements State {
        @Override
        public void close() {
            closeFuture(future);
        }
    }

    /**
     * Connecting to the remote server. This includes reading the body. A {@link #SUCCESS_EVENT}/{@link #FAILURE_EVENT}
     * has not been queued yet.
     *
     * @param channel The (possibly unresolved) channel.
     * @param handler The request handler. We keep a reference to this to dispose of the request body.
     * @see #connect(HttpUri, HttpMethod, NetworkUtils.ConnectionInfo, long)
     */
    private record Connecting(ChannelFuture channel, HttpRequestHandler handler) implements State {
        @Override
        public void close() {
            closeChannel(channel);
            handler.close();
        }
    }

    @Override
    protected void start() {
        transformStateAsync(Init.class, (s, f) -> new Lookup(f), s -> lookup(s.uri, s.method));
    }

    void redirect(URI uri, HttpMethod method) {
        HttpUri httpUri;
        try {
            httpUri = HttpRequest.checkUri(uri);
        } catch (HTTPRequestException e) {
            // If we cannot visit this uri, then fail.
            failure(NetworkUtils.toFriendlyError(e));
            return;
        }

        transformStateAsync(Connecting.class, (s, f) -> {
            s.close(); // Should be a no-op, but just in case.
            return new Lookup(f);
        }, s -> lookup(httpUri, method));
    }

    private StateTransition<State> lookup(HttpUri uri, HttpMethod method) {
        try {
            var conn = network.getConnectionInfo(uri, timeout);

            var requestBody = getHeaderSize(headers) + postBuffer.capacity();
            if (conn.options().maxUpload() != 0 && requestBody > conn.options().maxUpload()) {
                return StateTransition.close(() -> queueFailure("Request body is too large"));
            }

            return () -> connect(uri, method, conn, requestBody);
        } catch (HTTPRequestException e) {
            return StateTransition.close(() -> queueFailure(NetworkUtils.toFriendlyError(e)));
        } catch (Exception e) {
            LOG.error(Logging.HTTP_ERROR, "Error in HTTP request", e);
            return StateTransition.close(() -> queueFailure(NetworkUtils.toFriendlyError(e)));
        }
    }

    private State connect(HttpUri uri, HttpMethod method, NetworkUtils.ConnectionInfo conn, long requestBodySize) {
        // Add request size to the tracker before opening the connection
        environment.observe(Metrics.HTTP_REQUESTS);
        environment.observe(Metrics.HTTP_UPLOAD, requestBodySize);

        var handler = new HttpRequestHandler(this, uri.getUri(), method, conn.options());
        var connectFuture = network
            .connect(conn, ch -> {
                var p = ch.pipeline();
                if (timeout > 0) p.addLast(new ReadTimeoutHandler(timeout, TimeUnit.MILLISECONDS));

                p.addLast(
                    new HttpClientCodec(),
                    new HttpContentDecompressor(0),
                    handler
                );
            })
            .addListener(c -> {
                if (!c.isSuccess()) failure(NetworkUtils.toFriendlyError(c.cause()));
            });

        return new Connecting(connectFuture, handler);
    }

    private void queueFailure(String message) {
        environment.queueEvent(FAILURE_EVENT, address, message);
    }

    void failure(String message) {
        close(Connecting.class, () -> environment.queueEvent(FAILURE_EVENT, address, message));
    }

    void failure(String message, HttpResponseHandle object) {
        close(Connecting.class, () -> environment.queueEvent(FAILURE_EVENT, address, message, object));
    }

    void success(HttpResponseHandle object) {
        close(Connecting.class, () -> environment.queueEvent(SUCCESS_EVENT, address, object));
    }

    static long getHeaderSize(HttpHeaders headers) {
        long size = 0;
        for (var header : headers) {
            size += header.getKey() == null ? 0 : header.getKey().length();
            size += header.getValue() == null ? 0 : header.getValue().length() + 1;
        }
        return size;
    }

    ByteBuf body() {
        return postBuffer;
    }

    HttpHeaders headers() {
        return headers;
    }

    boolean isBinary() {
        return binary;
    }
}
