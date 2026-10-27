// SPDX-FileCopyrightText: 2019 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.core.apis.http.request;

import com.google.errorprone.annotations.concurrent.GuardedBy;
import dan200.computercraft.core.apis.handles.ArrayByteChannel;
import dan200.computercraft.core.apis.handles.ReadHandle;
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

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static dan200.computercraft.core.apis.http.request.HttpRequest.getHeaderSize;

final class HttpRequestHandler extends SimpleChannelInboundHandler<HttpObject> implements HttpBodyStream {
    /**
     * Same as {@link io.netty.handler.codec.MessageAggregator}.
     */
    private static final int DEFAULT_MAX_COMPOSITE_BUFFER_COMPONENTS = 1024;

    // TODO: make it be configurable
    // Note: this is not the real maximum buffer size. Reading will only stop after buffer exceed the size defined.
    private static final int MAX_STREAM_BUFFER_CACHE = 1024 * 1024;

    private static final byte[] EMPTY_BYTES = new byte[0];

    private final HttpRequest request;

    /**
     * Whether this particular request is still active. This is set to {@code false} when following a redirect, so that
     * we no longer queue events.
     */
    private boolean isActive = true;

    private final URI uri;
    private final HttpMethod method;
    private final boolean streaming;
    private final Options options;

    private final HttpHeaders responseHeaders = new DefaultHttpHeaders();
    private @Nullable HttpResponseStatus responseStatus;

    private final Object responseBodyLock = new Object();
    @GuardedBy("responseBodyLock")
    private @Nullable CompositeByteBuf responseBody;
    @GuardedBy("responseBodyLock")
    private @Nullable ByteBuf unpooledResponseBody;
    @GuardedBy("responseBodyLock")
    private @Nullable Runnable responseBodyTrigger;
    @GuardedBy("responseBodyLock")
    private boolean responseBodyDone = false;
    private volatile boolean waitingBody = false;

    HttpRequestHandler(HttpRequest request, URI uri, HttpMethod method, boolean streaming, Options options) {
        this.request = request;

        this.uri = uri;
        this.method = method;
        this.streaming = streaming;
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

            if (streaming) {
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
                    if (!streaming && options.maxDownload() != 0 && responseBody.readableBytes() + partial.readableBytes() > options.maxDownload()) {
                        // If we've read more than we're allowed to handle, abort as soon as possible.
                        isActive = false;
                        ctx.close();

                        request.failure("Response is too large");
                        return;
                    }
                    responseBody.addComponent(true, partial.retain());
                    if (streaming) {
                        if (responseBody.readableBytes() >= MAX_STREAM_BUFFER_CACHE) {
                            if (responseBody.readableBytes() >= MAX_STREAM_BUFFER_CACHE * 2) {
                                close();
                                ctx.close();
                                request.failure("Response buffer unexpectedly grew too large");
                                return;
                            }
                            // If AbstractTrafficShapingHandler is limiting read at the same time,
                            // autoRead will set to false before this http handler is invoked,
                            // so we can simply check if auto read is current active and
                            // let AbstractTrafficShapingHandler itself to resume the reading.
                            // If the traffic shaper resumed the reading while our responseBody still excess max buffer size,
                            // we will then setAutoRead to false.
                            //
                            // It is MOSTLY IMPOSSIBLE that AbstractTrafficShapingHandler limited read again in the next packet.
                            // However, that case still EXISTS, e.g. download speed is limited to a incredible small amount which
                            // lower than netty internal buffer size (1KB?), then it may cause unstoppable buffer grow.
                            // If that case should be handled properly, we should make our own implementation of AbstractTrafficShapingHandler
                            // which take care of our buffer size as well.
                            if (ctx.channel().config().isAutoRead()) {
                                ctx.channel().config().setAutoRead(false);
                                responseBodyTrigger = () -> ctx.executor().execute(() -> {
                                    if (!TrafficShapingHandlerAccess.isHandlerActive(ctx)) {
                                        ctx.channel().config().setAutoRead(true);
                                    }
                                });
                            }
                        }
                        if (waitingBody) {
                            waitingBody = false;
                            request.partialContent();
                        }
                    }
                }
            }

            if (message instanceof LastHttpContent last) {
                ctx.close();
                synchronized (responseBodyLock) {
                    responseBodyDone = true;

                    // TODO: we should have some way to provide trailing headers for streamed connection
                    if (!streaming) {
                        responseHeaders.add(last.trailingHeaders());

                        // Set the content length, if not already given.
                        if (!responseHeaders.contains(HttpHeaderNames.CONTENT_LENGTH)) {
                            responseHeaders.set(HttpHeaderNames.CONTENT_LENGTH, responseBody == null ? 0 : responseBody.readableBytes());
                        }

                        sendResponse();
                    } else {
                        request.partialClosed();
                    }
                }
            }
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        ctx.close();
        request.failure(NetworkUtils.toFriendlyError(cause));
    }

    private Map<String, String> decodeHeaders() {
        Map<String, String> headers = new HashMap<>();
        for (var header : responseHeaders) {
            var existing = headers.get(header.getKey());
            headers.put(header.getKey(), existing == null ? header.getValue() : existing + "," + header.getValue());
        }

        // Fire off a stats event
        request.environment().observe(Metrics.HTTP_DOWNLOAD, getHeaderSize(responseHeaders));
        return headers;
    }

    private void sendResponse() {
        Objects.requireNonNull(responseStatus, "Status has not been set");

        // Decode the headers
        var status = responseStatus;
        var headers = decodeHeaders();

        // Read the ByteBuf into a channel.
        byte[] bytes = EMPTY_BYTES;
        synchronized (responseBodyLock) {
            if (responseBody != null) {
                bytes = NetworkUtils.toBytes(responseBody);
                responseBody.release();
                responseBody = null;
            }
        }
        // Fire off a stats event
        request.environment().observe(Metrics.HTTP_DOWNLOAD, bytes.length);

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

        // Decode the headers
        var status = responseStatus;
        var headers = decodeHeaders();

        // Prepare to queue an event
        var reader = new HttpStreamReader(this, request.address, request.isBinary());
        var stream = new HttpResponseHandle(reader, status.code(), status.reasonPhrase(), headers);

        if (status.code() >= 200 && status.code() < 400) {
            request.partialSuccess(stream);
        } else {
            request.partialFailure(status.reasonPhrase(), stream);
        }
    }

    @Override
    public void closeStream() {
        request.partialClosed();
    }

    @Override
    public boolean isFinished() {
        synchronized (responseBodyLock) {
            return responseBodyDone;
        }
    }

    @Override
    public int getReadable() {
        synchronized (responseBodyLock) {
            var source = responseBody != null ? responseBody : unpooledResponseBody;
            return source == null ? 0 : source.readableBytes();
        }
    }

    @Override
    public int getMaxReadable() {
        return MAX_STREAM_BUFFER_CACHE;
    }

    @Override
    public void markWaiting() {
        waitingBody = true;
    }

    @Override
    public void read(ByteBuffer buffer) {
        synchronized (responseBodyLock) {
            var source = responseBody != null ? responseBody : unpooledResponseBody;
            if (source == null) return;

            source.readBytes(buffer);
            source.discardSomeReadBytes();
            triggerReread();
        }
    }

    /**
     * Read from the response buffer until a separator or end of file. The separator is included in the output buffer.
     * <p>
     * If the separator has not been reached, and bytes are still being received, this function returns {@code null},
     * indicating that the consumer should wait for more content to be received and try again.
     *
     * @param separator the target separator
     * @return The bytes which were read, or {@code null}.
     */
    @Override
    public @Nullable ByteBuffer readUntil(byte separator) {
        synchronized (responseBodyLock) {
            ByteBuf source = responseBody != null ? responseBody : unpooledResponseBody;
            if (source == null) return null; // Isn't this impossible? This should only happen if the channel is closed?

            var sepIndex = source.bytesBefore(separator);
            int toRead;
            if (sepIndex != -1) {
                toRead = sepIndex + 1;
            } else if (responseBodyDone) {
                toRead = source.readableBytes();
            } else {
                return null;
            }

            var buffer = ByteBuffer.allocateDirect(toRead);
            source.readBytes(buffer);
            source.discardSomeReadBytes();
            triggerReread();

            return buffer.flip();
        }
    }

    @GuardedBy("responseBodyLock")
    private void triggerReread() {
        if (responseBody != null && responseBodyTrigger != null && responseBody.readableBytes() < MAX_STREAM_BUFFER_CACHE) {
            responseBodyTrigger.run();
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
        synchronized (responseBodyLock) {
            responseBodyDone = true;
            responseBodyTrigger = null;
            if (responseBody != null) {
                if (streaming && responseBody.readableBytes() > 0) {
                    unpooledResponseBody = Unpooled.copiedBuffer(responseBody);
                }
                responseBody.release();
                responseBody = null;
            }
        }
    }
}
