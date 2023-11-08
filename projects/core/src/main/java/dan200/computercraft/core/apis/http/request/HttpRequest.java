// SPDX-FileCopyrightText: 2019 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.core.apis.http.request;

import dan200.computercraft.core.Logging;
import dan200.computercraft.core.apis.IAPIEnvironment;
import dan200.computercraft.core.apis.http.HTTPRequestException;
import dan200.computercraft.core.apis.http.NetworkUtils;
import dan200.computercraft.core.apis.http.Resource;
import dan200.computercraft.core.apis.http.ResourceGroup;
import dan200.computercraft.core.metrics.Metrics;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.handler.timeout.ReadTimeoutHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.util.Locale;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Represents an in-progress HTTP request.
 */
public class HttpRequest extends Resource<HttpRequest> {
    private static final Logger LOG = LoggerFactory.getLogger(HttpRequest.class);
    private static final String SUCCESS_EVENT = "http_success";
    private static final String FAILURE_EVENT = "http_failure";

    private static final int MAX_REDIRECTS = 16;

    private @Nullable Future<?> executorFuture;
    private @Nullable ChannelFuture connectFuture;
    private @Nullable HttpRequestHandler currentRequest;

    private final IAPIEnvironment environment;

    private final String address;
    private final ByteBuf postBuffer;
    private final HttpHeaders headers;
    private final boolean binary;
    private final int timeout;

    final AtomicInteger redirects;

    public HttpRequest(
        ResourceGroup<HttpRequest> limiter, IAPIEnvironment environment, String address, @Nullable ByteBuffer postBody,
        HttpHeaders headers, boolean binary, boolean followRedirects, int timeout
    ) {
        super(limiter);
        this.environment = environment;
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

    public static URI checkUri(String address) throws HTTPRequestException {
        URI url;
        try {
            url = new URI(address);
        } catch (URISyntaxException e) {
            throw new HTTPRequestException("URL malformed");
        }

        checkUri(url);
        return url;
    }

    public static void checkUri(URI url) throws HTTPRequestException {
        // Validate the URL
        if (url.getScheme() == null) throw new HTTPRequestException("Must specify http or https");
        if (url.getHost() == null) throw new HTTPRequestException("URL malformed");

        var scheme = url.getScheme().toLowerCase(Locale.ROOT);
        if (!scheme.equalsIgnoreCase("http") && !scheme.equalsIgnoreCase("https")) {
            throw new HTTPRequestException("Invalid protocol '" + scheme + "'");
        }
    }

    public void request(URI uri, HttpMethod method) {
        if (isClosed()) return;
        executorFuture = NetworkUtils.EXECUTOR.submit(() -> doRequest(uri, method));
        checkClosed();
    }

    private void doRequest(URI uri, HttpMethod method) {
        // If we're cancelled, abort.
        if (isClosed()) return;

        try {
            var ssl = uri.getScheme().equalsIgnoreCase("https");
            var socketAddress = NetworkUtils.getAddress(uri, ssl);
            var options = NetworkUtils.getOptions(uri.getHost(), socketAddress);
            var sslContext = ssl ? NetworkUtils.getSslContext() : null;
            var proxy = NetworkUtils.getProxyHandler(options, timeout);

            // getAddress may have a slight delay, so let's perform another cancellation check.
            if (isClosed()) return;

            var requestBody = getHeaderSize(headers) + postBuffer.capacity();
            if (options.maxUpload() != 0 && requestBody > options.maxUpload()) {
                failure("Request body is too large");
                return;
            }

            // Add request size to the tracker before opening the connection
            environment.observe(Metrics.HTTP_REQUESTS);
            environment.observe(Metrics.HTTP_UPLOAD, requestBody);

            var handler = currentRequest = new HttpRequestHandler(this, uri, method, options);
            connectFuture = new Bootstrap()
                .group(NetworkUtils.LOOP_GROUP)
                .channelFactory(NioSocketChannel::new)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        NetworkUtils.initChannel(ch, uri, socketAddress, sslContext, proxy, timeout);

                        var p = ch.pipeline();
                        if (timeout > 0) p.addLast(new ReadTimeoutHandler(timeout, TimeUnit.MILLISECONDS));

                        p.addLast(
                            new HttpClientCodec(),
                            new HttpContentDecompressor(),
                            handler
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
            LOG.error(Logging.HTTP_ERROR, "Error in HTTP request", e);
        }
    }

    void failure(String message) {
        if (tryClose()) environment.queueEvent(FAILURE_EVENT, address, message);
    }

    void failure(String message, HttpResponseHandle object) {
        if (tryClose()) environment.queueEvent(FAILURE_EVENT, address, message, object);
    }

    void success(HttpResponseHandle object) {
        if (tryClose()) environment.queueEvent(SUCCESS_EVENT, address, object);
    }

    @Override
    protected void dispose() {
        super.dispose();

        executorFuture = closeFuture(executorFuture);
        connectFuture = closeChannel(connectFuture);
        currentRequest = closeCloseable(currentRequest);
    }

    public static long getHeaderSize(HttpHeaders headers) {
        long size = 0;
        for (var header : headers) {
            size += header.getKey() == null ? 0 : header.getKey().length();
            size += header.getValue() == null ? 0 : header.getValue().length() + 1;
        }
        return size;
    }

    public ByteBuf body() {
        return postBuffer;
    }

    public HttpHeaders headers() {
        return headers;
    }

    public boolean isBinary() {
        return binary;
    }
}
