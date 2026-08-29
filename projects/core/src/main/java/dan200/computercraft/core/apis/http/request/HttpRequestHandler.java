// SPDX-FileCopyrightText: 2019 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.core.apis.http.request;

import dan200.computercraft.core.apis.handles.ArrayByteChannel;
import dan200.computercraft.core.apis.handles.ReadHandle;
import dan200.computercraft.core.apis.http.HTTPRequestException;
import dan200.computercraft.core.apis.http.NetworkUtils;
import dan200.computercraft.core.apis.http.options.Options;
import dan200.computercraft.core.metrics.Metrics;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.CompositeByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;
import org.jspecify.annotations.Nullable;

import java.io.Closeable;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static dan200.computercraft.core.apis.http.request.HttpRequest.getHeaderSize;

public final class HttpRequestHandler extends SimpleChannelInboundHandler<HttpObject> implements Closeable {
    /**
     * Same as {@link io.netty.handler.codec.MessageAggregator}.
     */
    private static final int DEFAULT_MAX_COMPOSITE_BUFFER_COMPONENTS = 1024;

    // TODO: make it be configurable
    // Note: this is not the real maximum buffer size. Reading will only stop after buffer exceed the size defined.
    private static final int MAX_STREAM_BUFFER_CACHE = 1024 * 1024;

    private static final byte[] EMPTY_BYTES = new byte[0];

    private final HttpRequest request;
    private boolean closed = false;

    private final URI uri;
    private final HttpMethod method;
    private final boolean stream;
    private final Options options;

    private @Nullable Charset responseCharset;
    private final HttpHeaders responseHeaders = new DefaultHttpHeaders();
    private @Nullable HttpResponseStatus responseStatus;

    private final Object responseBodyLock = new Object();
    private @Nullable CompositeByteBuf responseBody;
    private @Nullable ByteBuf unpooledResponseBody;
    private @Nullable Runnable responseBodyTrigger;

    HttpRequestHandler(HttpRequest request, URI uri, HttpMethod method, boolean stream, Options options) {
        this.request = request;

        this.uri = uri;
        this.method = method;
        this.stream = stream;
        this.options = options;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        if (request.checkClosed()) return;

        var body = request.body();
        body.resetReaderIndex().retain();

        var requestUri = uri.getRawPath();
        if (uri.getRawQuery() != null) requestUri += "?" + uri.getRawQuery();

        FullHttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, requestUri, body);
        request.setMethod(method);
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
        if (!closed) request.failure("Could not connect");
        super.channelInactive(ctx);
    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, HttpObject message) {
        if (closed || request.checkClosed()) return;

        if (message instanceof HttpResponse response) {

            if (request.redirects.get() > 0) {
                var redirect = getRedirect(response.status(), response.headers());
                if (redirect != null && !uri.equals(redirect) && request.redirects.getAndDecrement() > 0) {
                    // If we have a redirect, and don't end up at the same place, then follow it.

                    // We mark ourselves as disposed first though, to avoid firing events when the channel
                    // becomes inactive or disposed.
                    closed = true;
                    ctx.close();

                    try {
                        HttpRequest.checkUri(redirect);
                    } catch (HTTPRequestException e) {
                        // If we cannot visit this uri, then fail.
                        request.failure(NetworkUtils.toFriendlyError(e));
                        return;
                    }

                    request.request(redirect, response.status().code() == 303 ? HttpMethod.GET : method);
                    return;
                }
            }

            responseCharset = HttpUtil.getCharset(response, StandardCharsets.UTF_8);
            responseStatus = response.status();
            responseHeaders.add(response.headers());

            if (stream) {
                sendResponseStreamed();
            }
        }

        if (message instanceof HttpContent content) {

            var partial = content.content();
            if (partial.isReadable()) {
                synchronized (responseBodyLock) {
                    if (responseBody == null) {
                        responseBody = ctx.alloc().compositeBuffer(DEFAULT_MAX_COMPOSITE_BUFFER_COMPONENTS);
                    }
                    if (!stream && options.maxDownload() != 0 && responseBody.readableBytes() + partial.readableBytes() > options.maxDownload()) {
                        // If we've read more than we're allowed to handle, abort as soon as possible.
                        closed = true;
                        ctx.close();

                        request.failure("Response is too large");
                        return;
                    }
                    var wasEmpty = !responseBody.isReadable();
                    responseBody.addComponent(true, partial.retain());
                    if (stream) {
                        if (wasEmpty) {
                            request.partialContent();
                        }
                        if (responseBody.readableBytes() >= MAX_STREAM_BUFFER_CACHE) {
                            ctx.channel().config().setAutoRead(false);
                        }
                        responseBodyTrigger = () -> ctx.channel().config().setAutoRead(true);
                    }
                }
            }

            if (message instanceof LastHttpContent last) {
                ctx.close();

                // TODO: we should have some way to provide trailing headers for streamed connection
                if (!stream) {
                    responseHeaders.add(last.trailingHeaders());

                    // Set the content length, if not already given.
                    if (!responseHeaders.contains(HttpHeaderNames.CONTENT_LENGTH)) {
                        responseHeaders.set(HttpHeaderNames.CONTENT_LENGTH, responseBody == null ? 0 : responseBody.readableBytes());
                    }

                    sendResponse();
                }
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
        Objects.requireNonNull(responseCharset, "Charset has not been set");

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

    private void sendResponseStreamed() {
        Objects.requireNonNull(responseStatus, "Status has not been set");
        Objects.requireNonNull(responseCharset, "Charset has not been set");

        // Decode the headers
        var status = responseStatus;
        Map<String, String> headers = new HashMap<>();
        for (var header : responseHeaders) {
            var existing = headers.get(header.getKey());
            headers.put(header.getKey(), existing == null ? header.getValue() : existing + "," + header.getValue());
        }

        // Fire off a stats event
        request.environment().observe(Metrics.HTTP_DOWNLOAD, getHeaderSize(responseHeaders));

        // Prepare to queue an event
        var stream = new HttpResponseHandle(new HttpStreamReader(request, this), status.code(), status.reasonPhrase(), headers);

        if (status.code() >= 200 && status.code() < 400) {
            request.partialSuccess(stream);
        } else {
            request.partialFailure(status.reasonPhrase(), stream);
        }
    }

    /**
     * Read response body into the designate buffer.
     * This method does not block, and will only read currently cached data into the buffer and returns the amount of available bytes.
     *
     * @param buffer the designate buffer
     * @return The amount of bytes read, or {@code -1} if stream is ended.
     */
    int readBody(ByteBuffer buffer) {
        synchronized (responseBodyLock) {
            ByteBuf source = responseBody != null ? responseBody : unpooledResponseBody;
            if (source == null) return -1;
            int maxRead = buffer.remaining();
            int oldLimit = -1;
            if (maxRead > source.readableBytes()) {
                maxRead = source.readableBytes();
                oldLimit = buffer.limit();
                buffer.limit(buffer.position() + maxRead);
            }
            source.readBytes(buffer);
            if (oldLimit != -1) {
                buffer.limit(oldLimit);
            }
            if (source == responseBody) {
                responseBody.discardReadComponents();
                if (responseBodyTrigger != null && responseBody.readableBytes() < MAX_STREAM_BUFFER_CACHE) {
                    responseBodyTrigger.run();
                    responseBodyTrigger = null;
                }
            } else if (source == unpooledResponseBody) {
                if (unpooledResponseBody.readableBytes() == 0) {
                    unpooledResponseBody = null;
                }
            }
            return maxRead;
        }
    }

    /**
     * Read response body into the designate buffer until the specific separator.
     * This method does not block, and will only read currently cached data into the buffer and returns the amount of available bytes.
     * Separator will be read into the buffer, and if exists, it will always and only appears at the buffer's last position.
     *
     * @param buffer the designate buffer
     * @param separator the target separator
     * @return The amount of bytes read, or {@code -1} if stream is ended.
     */
    int readBodyUntil(ByteBuffer buffer, byte separator) {
        synchronized (responseBodyLock) {
            ByteBuf source = responseBody != null ? responseBody : unpooledResponseBody;
            if (source == null) return -1;
            int maxRead = buffer.remaining();
            if (maxRead > source.readableBytes()) maxRead = source.readableBytes();
            int sepIndex = source.bytesBefore(maxRead, separator);
            if (sepIndex != -1) {
                maxRead = sepIndex + 1;
            }
            int oldLimit = -1;
            if (maxRead != buffer.remaining()) {
                oldLimit = buffer.limit();
                buffer.limit(buffer.position() + maxRead);
            }

            source.readBytes(buffer);

            if (oldLimit != -1) {
                buffer.limit(oldLimit);
            }
            if (source == responseBody) {
                responseBody.discardReadComponents();
                if (responseBodyTrigger != null && responseBody.readableBytes() < MAX_STREAM_BUFFER_CACHE) {
                    responseBodyTrigger.run();
                    responseBodyTrigger = null;
                }
            } else if (source == unpooledResponseBody) {
                if (unpooledResponseBody.readableBytes() == 0) {
                    unpooledResponseBody = null;
                }
            }
            return maxRead;
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

    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        synchronized (responseBodyLock) {
            responseBodyTrigger = null;
            if (responseBody != null) {
                if (stream && responseBody.readableBytes() > 0) {
                    unpooledResponseBody = Unpooled.copiedBuffer(responseBody);
                }
                responseBody.release();
                responseBody = null;
            }
        }
    }
}
