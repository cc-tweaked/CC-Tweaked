// SPDX-FileCopyrightText: 2019 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.core.apis.http.request;

import dan200.computercraft.core.apis.handles.ArrayByteChannel;
import dan200.computercraft.core.apis.handles.ReadHandle;
import dan200.computercraft.core.apis.http.NetworkUtils;
import dan200.computercraft.core.apis.http.options.Options;
import dan200.computercraft.core.metrics.Metrics;
import io.netty.buffer.CompositeByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;
import org.jspecify.annotations.Nullable;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static dan200.computercraft.core.apis.http.request.HttpRequest.getHeaderSize;

final class HttpRequestHandler extends SimpleChannelInboundHandler<HttpObject> {
    /**
     * Same as {@link io.netty.handler.codec.MessageAggregator}.
     */
    private static final int DEFAULT_MAX_COMPOSITE_BUFFER_COMPONENTS = 1024;

    private static final byte[] EMPTY_BYTES = new byte[0];

    private final HttpRequest request;

    /**
     * Whether this particular request is still active. This is set to {@code false} when following a redirect, so that
     * we no longer queue events.
     */
    private boolean isActive = true;

    private final URI uri;
    private final HttpMethod method;
    private final Options options;

    private final HttpHeaders responseHeaders = new DefaultHttpHeaders();
    private @Nullable HttpResponseStatus responseStatus;
    private @Nullable CompositeByteBuf responseBody;

    HttpRequestHandler(HttpRequest request, URI uri, HttpMethod method, Options options) {
        this.request = request;

        this.uri = uri;
        this.method = method;
        this.options = options;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        if (request.isClosed()) return;

        var body = request.body();
        body.resetReaderIndex().retain();

        var requestUri = uri.getRawPath();
        if (uri.getRawQuery() != null) requestUri += "?" + uri.getRawQuery();

        FullHttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, method, requestUri, body);
        request.headers().set(this.request.headers());

        // We force some headers to be always applied
        if (!request.headers().contains(HttpHeaderNames.ACCEPT_CHARSET)) {
            request.headers().set(HttpHeaderNames.ACCEPT_CHARSET, "UTF-8");
        }
        request.headers().set(HttpHeaderNames.HOST, uri.getPort() < 0 ? uri.getHost() : uri.getHost() + ":" + uri.getPort());
        request.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE);

        ctx.channel().writeAndFlush(request);

        super.channelActive(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        if (isActive) request.failure("Could not connect");
        super.channelInactive(ctx);
    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, HttpObject message) {
        if (!isActive || request.isClosed()) return;

        if (message instanceof HttpResponse response) {

            if (request.redirects.get() > 0) {
                var redirect = getRedirect(response.status(), response.headers());
                if (redirect != null && !uri.equals(redirect) && request.redirects.getAndDecrement() > 0) {
                    // If we have a redirect, and don't end up at the same place, then follow it.

                    // We mark ourselves as no longer active first, to avoid firing events when the channel becomes
                    // inactive or is closed.
                    isActive = false;
                    ctx.close();

                    request.redirect(redirect, response.status().code() == 303 ? HttpMethod.GET : method);
                    return;
                }
            }

            responseStatus = response.status();
            responseHeaders.add(response.headers());
        }

        if (message instanceof HttpContent content) {

            if (responseBody == null) {
                responseBody = ctx.alloc().compositeBuffer(DEFAULT_MAX_COMPOSITE_BUFFER_COMPONENTS);
            }

            var partial = content.content();
            if (partial.isReadable()) {
                // If we've read more than we're allowed to handle, abort as soon as possible.
                if (options.maxDownload() != 0 && responseBody.readableBytes() + partial.readableBytes() > options.maxDownload()) {
                    isActive = false;
                    ctx.close();

                    request.failure("Response is too large");
                    return;
                }

                responseBody.addComponent(true, partial.retain());
            }

            if (message instanceof LastHttpContent last) {
                responseHeaders.add(last.trailingHeaders());

                // Set the content length, if not already given.
                if (responseHeaders.contains(HttpHeaderNames.CONTENT_LENGTH)) {
                    responseHeaders.set(HttpHeaderNames.CONTENT_LENGTH, responseBody.readableBytes());
                }

                ctx.close();
                sendResponse();
            }
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        ctx.close();
        request.failure(NetworkUtils.toFriendlyError(cause));
    }

    private void sendResponse() {
        Objects.requireNonNull(responseStatus, "Status has not been set");

        // Read the ByteBuf into a channel.
        var body = responseBody;
        var bytes = body == null ? EMPTY_BYTES : NetworkUtils.toBytes(body);

        // Decode the headers
        var status = responseStatus;
        Map<String, String> headers = new HashMap<>();
        for (var header : responseHeaders) {
            var existing = headers.get(header.getKey());
            headers.put(header.getKey(), existing == null ? header.getValue() : existing + "," + header.getValue());
        }

        // Fire off a stats event
        request.environment().observe(Metrics.HTTP_DOWNLOAD, getHeaderSize(responseHeaders) + bytes.length);

        // Prepare to queue an event
        var contents = new ArrayByteChannel(bytes);
        var reader = new ReadHandle(contents, request.isBinary());
        var stream = new HttpResponseHandle(reader, status.code(), status.reasonPhrase(), headers);

        if (status.code() >= 200 && status.code() < 400) {
            request.success(stream);
        } else {
            request.failure(status.reasonPhrase(), stream);
        }
    }

    /**
     * Determine the redirect from this response.
     *
     * @param status  The status of the HTTP response.
     * @param headers The headers of the HTTP response.
     * @return The URI to redirect to, or {@code null} if no redirect should occur.
     */
    @Nullable
    private URI getRedirect(HttpResponseStatus status, HttpHeaders headers) {
        var code = status.code();
        if (code < 300 || code > 307 || code == 304 || code == 306) return null;

        var location = headers.get(HttpHeaderNames.LOCATION);
        if (location == null) return null;

        try {
            return uri.resolve(new URI(location));
        } catch (IllegalArgumentException | URISyntaxException e) {
            return null;
        }
    }

    void close() {
        isActive = false;
        if (responseBody != null) {
            responseBody.release();
            responseBody = null;
        }
    }
}
