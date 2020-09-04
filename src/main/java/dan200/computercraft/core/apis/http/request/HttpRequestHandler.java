/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.core.apis.http.request;

import static dan200.computercraft.core.apis.http.request.HttpRequest.getHeaderSize;

import java.io.Closeable;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.core.apis.handles.ArrayByteChannel;
import dan200.computercraft.core.apis.handles.BinaryReadableHandle;
import dan200.computercraft.core.apis.handles.EncodedReadableHandle;
import dan200.computercraft.core.apis.handles.HandleGeneric;
import dan200.computercraft.core.apis.http.HTTPRequestException;
import dan200.computercraft.core.apis.http.NetworkUtils;
import dan200.computercraft.core.apis.http.options.Options;
import dan200.computercraft.core.tracking.TrackingField;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.CompositeByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.LastHttpContent;

public final class HttpRequestHandler extends SimpleChannelInboundHandler<HttpObject> implements Closeable {
    /**
     * Same as {@link io.netty.handler.codec.MessageAggregator}.
     */
    private static final int DEFAULT_MAX_COMPOSITE_BUFFER_COMPONENTS = 1024;

    private static final byte[] EMPTY_BYTES = new byte[0];

    private final HttpRequest request;
    private final URI uri;
    private final HttpMethod method;
    private final Options options;
    private final HttpHeaders responseHeaders = new DefaultHttpHeaders();
    private boolean closed = false;
    private Charset responseCharset;
    private HttpResponseStatus responseStatus;
    private CompositeByteBuf responseBody;

    HttpRequestHandler(HttpRequest request, URI uri, HttpMethod method, Options options) {
        this.request = request;

        this.uri = uri;
        this.method = method;
        this.options = options;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        if (this.request.checkClosed()) {
            return;
        }

        ByteBuf body = this.request.body();
        body.resetReaderIndex()
            .retain();

        String requestUri = this.uri.getRawPath();
        if (this.uri.getRawQuery() != null) {
            requestUri += "?" + this.uri.getRawQuery();
        }

        FullHttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, requestUri, body);
        request.setMethod(this.method);
        request.headers()
               .set(this.request.headers());

        // We force some headers to be always applied
        if (!request.headers()
                    .contains(HttpHeaderNames.ACCEPT_CHARSET)) {
            request.headers()
                   .set(HttpHeaderNames.ACCEPT_CHARSET, "UTF-8");
        }
        if (!request.headers()
                    .contains(HttpHeaderNames.USER_AGENT)) {
            request.headers()
                   .set(HttpHeaderNames.USER_AGENT,
                        this.request.environment()
                                    .getComputerEnvironment()
                                    .getUserAgent());
        }
        request.headers()
               .set(HttpHeaderNames.HOST, this.uri.getPort() < 0 ? this.uri.getHost() : this.uri.getHost() + ":" + this.uri.getPort());
        request.headers()
               .set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE);

        ctx.channel()
           .writeAndFlush(request);

        super.channelActive(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        if (!this.closed) {
            this.request.failure("Could not connect");
        }
        super.channelInactive(ctx);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        if (ComputerCraft.logPeripheralErrors) {
            ComputerCraft.log.error("Error handling HTTP response", cause);
        }
        this.request.failure(cause);
    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, HttpObject message) {
        if (this.closed || this.request.checkClosed()) {
            return;
        }

        if (message instanceof HttpResponse) {
            HttpResponse response = (HttpResponse) message;

            if (this.request.redirects.get() > 0) {
                URI redirect = this.getRedirect(response.status(), response.headers());
                if (redirect != null && !this.uri.equals(redirect) && this.request.redirects.getAndDecrement() > 0) {
                    // If we have a redirect, and don't end up at the same place, then follow it.

                    // We mark ourselves as disposed first though, to avoid firing events when the channel
                    // becomes inactive or disposed.
                    this.closed = true;
                    ctx.close();

                    try {
                        HttpRequest.checkUri(redirect);
                    } catch (HTTPRequestException e) {
                        // If we cannot visit this uri, then fail.
                        this.request.failure(e.getMessage());
                        return;
                    }

                    this.request.request(redirect,
                                    response.status()
                                            .code() == 303 ? HttpMethod.GET : this.method);
                    return;
                }
            }

            this.responseCharset = HttpUtil.getCharset(response, StandardCharsets.UTF_8);
            this.responseStatus = response.status();
            this.responseHeaders.add(response.headers());
        }

        if (message instanceof HttpContent) {
            HttpContent content = (HttpContent) message;

            if (this.responseBody == null) {
                this.responseBody = ctx.alloc()
                                       .compositeBuffer(DEFAULT_MAX_COMPOSITE_BUFFER_COMPONENTS);
            }

            ByteBuf partial = content.content();
            if (partial.isReadable()) {
                // If we've read more than we're allowed to handle, abort as soon as possible.
                if (this.options.maxDownload != 0 && this.responseBody.readableBytes() + partial.readableBytes() > this.options.maxDownload) {
                    this.closed = true;
                    ctx.close();

                    this.request.failure("Response is too large");
                    return;
                }

                this.responseBody.addComponent(true, partial.retain());
            }

            if (message instanceof LastHttpContent) {
                LastHttpContent last = (LastHttpContent) message;
                this.responseHeaders.add(last.trailingHeaders());

                // Set the content length, if not already given.
                if (this.responseHeaders.contains(HttpHeaderNames.CONTENT_LENGTH)) {
                    this.responseHeaders.set(HttpHeaderNames.CONTENT_LENGTH, this.responseBody.readableBytes());
                }

                ctx.close();
                this.sendResponse();
            }
        }
    }

    /**
     * Determine the redirect from this response.
     *
     * @param status The status of the HTTP response.
     * @param headers The headers of the HTTP response.
     * @return The URI to redirect to, or {@code null} if no redirect should occur.
     */
    private URI getRedirect(HttpResponseStatus status, HttpHeaders headers) {
        int code = status.code();
        if (code < 300 || code > 307 || code == 304 || code == 306) {
            return null;
        }

        String location = headers.get(HttpHeaderNames.LOCATION);
        if (location == null) {
            return null;
        }

        try {
            return this.uri.resolve(new URI(URLDecoder.decode(location, "UTF-8")));
        } catch (UnsupportedEncodingException | IllegalArgumentException | URISyntaxException e) {
            return null;
        }
    }

    private void sendResponse() {
        // Read the ByteBuf into a channel.
        CompositeByteBuf body = this.responseBody;
        byte[] bytes = body == null ? EMPTY_BYTES : NetworkUtils.toBytes(body);

        // Decode the headers
        HttpResponseStatus status = this.responseStatus;
        Map<String, String> headers = new HashMap<>();
        for (Map.Entry<String, String> header : this.responseHeaders) {
            String existing = headers.get(header.getKey());
            headers.put(header.getKey(), existing == null ? header.getValue() : existing + "," + header.getValue());
        }

        // Fire off a stats event
        this.request.environment()
                    .addTrackingChange(TrackingField.HTTP_DOWNLOAD, getHeaderSize(this.responseHeaders) + bytes.length);

        // Prepare to queue an event
        ArrayByteChannel contents = new ArrayByteChannel(bytes);
        HandleGeneric reader = this.request.isBinary() ? BinaryReadableHandle.of(contents) : new EncodedReadableHandle(EncodedReadableHandle.open(contents,
                                                                                                                                                  this.responseCharset));
        HttpResponseHandle stream = new HttpResponseHandle(reader, status.code(), status.reasonPhrase(), headers);

        if (status.code() >= 200 && status.code() < 400) {
            this.request.success(stream);
        } else {
            this.request.failure(status.reasonPhrase(), stream);
        }
    }

    @Override
    public void close() {
        this.closed = true;
        if (this.responseBody != null) {
            this.responseBody.release();
            this.responseBody = null;
        }
    }
}
