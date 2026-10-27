// SPDX-FileCopyrightText: 2023 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package cc.tweaked.web.http;

import cc.tweaked.web.Main;
import cc.tweaked.web.js.abort.AbortController;
import cc.tweaked.web.js.fetch.Headers;
import cc.tweaked.web.js.fetch.RequestInit;
import cc.tweaked.web.js.fetch.Response;
import dan200.computercraft.core.apis.IAPIEnvironment;
import dan200.computercraft.core.apis.handles.ArrayByteChannel;
import dan200.computercraft.core.apis.handles.ReadHandle;
import dan200.computercraft.core.apis.http.HttpUri;
import dan200.computercraft.core.apis.http.Resource;
import dan200.computercraft.core.apis.http.ResourceGroup;
import dan200.computercraft.core.apis.http.request.HttpBodyStream;
import dan200.computercraft.core.apis.http.request.HttpResponseHandle;
import dan200.computercraft.core.apis.http.request.HttpStreamReader;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.teavm.jso.JSObject;
import org.teavm.jso.ajax.XMLHttpRequest;
import org.teavm.jso.streams.ReadableStreamDefaultReader;
import org.teavm.jso.typedarrays.Int8Array;

import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;

/**
 * Replaces {@link dan200.computercraft.core.apis.http.request.HttpRequest} with a version which uses AJAX/{@link XMLHttpRequest}.
 */
final class HttpRequest extends Resource<HttpRequest, HttpRequest.State> {
    private static final Logger LOG = LoggerFactory.getLogger(HttpRequest.class);
    private static final String SUCCESS_EVENT = "http_success";
    private static final String FAILURE_EVENT = "http_failure";
    private static final String CONTENT_EVENT = "http_content";

    private final IAPIEnvironment environment;

    private final String address;
    private final @Nullable ByteBuffer postBuffer;
    private final HttpHeaders headers;
    private final boolean binary;
    private final boolean followRedirects;
    private final boolean streaming;

    HttpRequest(
        ResourceGroup<HttpRequest> limiter, IAPIEnvironment environment,
        String address, HttpUri uri, HttpMethod method, @Nullable ByteBuffer postBody, HttpHeaders headers,
        boolean binary, boolean followRedirects, boolean streaming
    ) {
        super(limiter, new Init(uri, method));
        this.environment = environment;
        this.address = address;
        postBuffer = postBody;
        this.headers = headers;
        this.binary = binary;
        this.followRedirects = followRedirects;
        this.streaming = streaming;

        if (postBody != null) {
            if (!headers.contains(HttpHeaderNames.CONTENT_TYPE)) {
                headers.set(HttpHeaderNames.CONTENT_TYPE, "application/x-www-form-urlencoded; charset=utf-8");
            }
        }
    }

    public sealed interface State extends ResourceState {
    }

    private record Init(HttpUri uri, HttpMethod method) implements State {
        @Override
        public void close() {
        }
    }

    private record Fetching(AbortController abort) implements State {
        @Override
        public void close() {
            abort.abort();
        }
    }

    @Override
    protected void start() {
        transformState(Init.class, s -> doRequest(s.uri, s.method));
    }

    private @Nullable State doRequest(HttpUri uri, HttpMethod method) {
        try {
            var abort = new AbortController();

            var request = RequestInit.create();
            request.setMethod(method.name());
            if (postBuffer != null) request.setBody(Int8Array.fromJavaBuffer(postBuffer));
            request.setSignal(abort.getSignal());

            var requestHeaders = new Headers();
            for (var iterator = headers.iteratorAsString(); iterator.hasNext(); ) {
                var header = iterator.next();
                requestHeaders.append(header.getKey(), header.getValue());
            }
            requestHeaders.append("X-CC-Redirect", Boolean.toString(followRedirects));

            var address = uri.getUri().toASCIIString();
            Response.fetch(Main.CORS_PROXY.isEmpty() ? address : Main.CORS_PROXY.replace("{}", address), request)
                .then(this::handleResponse)
                .catchError(e -> {
                    LOG.error("Error in HTTP request", e);
                    close(Fetching.class, () -> queueFailure("Could not connect"));
                    return null;
                });

            return new Fetching(abort);
        } catch (Exception e) {
            LOG.error("Error in HTTP request", e);
            queueFailure("Could not connect");
            return null;
        }
    }

    private @Nullable JSObject handleResponse(Response response) {
        if (response.getStatus() == 0) {
            close(Fetching.class, () -> queueFailure("Could not connect"));
            return null;
        }

        Map<String, String> responseHeaders = new HashMap<>();
        response.getHeaders().forEach((v, k, h) -> {
            // Normalise the header (so "content-type" becomes "Content-Type")
            var upcase = true;
            var headerBuilder = new StringBuilder(k.length());
            for (var i = 0; i < k.length(); i++) {
                var c = k.charAt(i);
                headerBuilder.append(upcase ? Character.toUpperCase(c) : c);
                upcase = c == '-';
            }
            responseHeaders.put(headerBuilder.toString(), v);
        });

        if (streaming) {
            var reader = new HttpStreamReader(new StreamReader(response.getBody().getReader()), address, binary);
            var stream = new HttpResponseHandle(reader, response.getStatus(), response.getStatusText(), responseHeaders);

            if (response.getStatus() >= 200 && response.getStatus() < 400) {
                environment.queueEvent(SUCCESS_EVENT, address, stream);
            } else {
                environment.queueEvent(FAILURE_EVENT, address, response.getStatusText(), stream);
            }

            return null;
        } else {
            return response.bytes().then(body -> {
                SeekableByteChannel contents = new ArrayByteChannel(new Int8Array(body).copyToJavaArray());
                var reader = new ReadHandle(contents, binary);

                var stream = new HttpResponseHandle(reader, response.getStatus(), response.getStatusText(), responseHeaders);

                if (response.getStatus() >= 200 && response.getStatus() < 400) {
                    close(Fetching.class, () -> environment.queueEvent(SUCCESS_EVENT, address, stream));
                } else {
                    close(Fetching.class, () -> environment.queueEvent(FAILURE_EVENT, address, response.getStatusText(), stream));
                }

                return null;
            });
        }
    }

    private void queueFailure(String message) {
        environment.queueEvent(FAILURE_EVENT, address, message);
    }

    private final class StreamReader implements HttpBodyStream {
        private static final int MAX_SIZE = 1024 * 1024;

        private final ReadableStreamDefaultReader reader;
        private boolean isReading;
        private boolean isFinished;

        private final Queue<ByteBuffer> buffers = new ArrayDeque<>();
        private int bufferSize = 0;

        private boolean waiting;

        private StreamReader(ReadableStreamDefaultReader reader) {
            this.reader = reader;
            startRead();
        }

        private void startRead() {
            if (isReading || bufferSize >= MAX_SIZE) return;

            reader.read().then(x -> {
                isReading = false;

                if (waiting) {
                    environment.queueEvent(CONTENT_EVENT, address);
                    waiting = false;
                }

                if (x.isDone()) {
                    isFinished = true;
                } else {
                    var buffer = ByteBuffer.wrap(x.getValue().copyToJavaArray()).asReadOnlyBuffer();
                    buffers.add(buffer);
                    bufferSize += buffer.remaining();

                    startRead();
                }

                return null;
            }).catchError(e -> {
                LOG.error("Failed to read HTTP response", e);
                closeStream();
                return null;
            });
            isReading = true;
        }

        @Override
        public void closeStream() {
            close(Fetching.class, () -> environment.queueEvent(CONTENT_EVENT, address));
        }

        @Override
        public boolean isFinished() {
            return isFinished;
        }

        @Override
        public int getReadable() {
            return bufferSize;
        }

        @Override
        public int getMaxReadable() {
            return MAX_SIZE;
        }

        @Override
        public void markWaiting() {
            waiting = true;
        }

        @Override
        public void read(ByteBuffer buffer) {
            while (true) {
                var source = buffers.peek();
                if (source == null) break;

                // Read out of the head buffer, and decrease our buffer size by what was read.
                var remaining = source.remaining();
                buffer.put(source);
                bufferSize -= remaining - source.remaining();

                if (!source.hasRemaining()) {
                    buffers.remove(); // Source buffer was exhausted, remove it.
                } else if (!buffer.hasRemaining()) {
                    break; // Destination buffer is full, exit.
                } else {
                    throw new IllegalStateException("Buffer has remaining capacity but was not exhausted");
                }
            }

            startRead();
        }

        @Override
        public @Nullable ByteBuffer readUntil(byte separator) {
            var toRead = 0;
            var found = false;

            outer:
            while (true) {
                var source = buffers.peek();
                if (source == null) break;

                for (int i = source.position(), len = source.limit(); i < len; i++) {
                    toRead += 1;
                    if (source.get(i) == separator) {
                        found = true;
                        break outer;
                    }
                }
            }

            return found || isFinished ? read(toRead) : null;
        }
    }
}
