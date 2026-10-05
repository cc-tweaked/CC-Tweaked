// SPDX-FileCopyrightText: 2023 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package cc.tweaked.web.http;

import cc.tweaked.web.Main;
import com.google.common.base.Splitter;
import dan200.computercraft.core.Logging;
import dan200.computercraft.core.apis.IAPIEnvironment;
import dan200.computercraft.core.apis.handles.ArrayByteChannel;
import dan200.computercraft.core.apis.handles.ReadHandle;
import dan200.computercraft.core.apis.http.Resource;
import dan200.computercraft.core.apis.http.ResourceGroup;
import dan200.computercraft.core.apis.http.request.HttpResponseHandle;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.teavm.jso.ajax.XMLHttpRequest;
import org.teavm.jso.typedarrays.ArrayBuffer;
import org.teavm.jso.typedarrays.Int8Array;

import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.util.HashMap;
import java.util.Map;

/**
 * Replaces {@link dan200.computercraft.core.apis.http.request.HttpRequest} with a version which uses AJAX/{@link XMLHttpRequest}.
 */
final class HttpRequest extends Resource<HttpRequest> {
    private static final Logger LOG = LoggerFactory.getLogger(HttpRequest.class);
    private static final String SUCCESS_EVENT = "http_success";
    private static final String FAILURE_EVENT = "http_failure";

    private final IAPIEnvironment environment;

    private final String address;
    private final @Nullable ByteBuffer postBuffer;
    private final HttpHeaders headers;
    private final boolean binary;
    private final boolean followRedirects;

    HttpRequest(
        ResourceGroup<HttpRequest> limiter, IAPIEnvironment environment, String address, @Nullable ByteBuffer postBody,
        HttpHeaders headers, boolean binary, boolean followRedirects
    ) {
        super(limiter);
        this.environment = environment;
        this.address = address;
        postBuffer = postBody;
        this.headers = headers;
        this.binary = binary;
        this.followRedirects = followRedirects;

        if (postBody != null) {
            if (!headers.contains(HttpHeaderNames.CONTENT_TYPE)) {
                headers.set(HttpHeaderNames.CONTENT_TYPE, "application/x-www-form-urlencoded; charset=utf-8");
            }
        }
    }

    public void request(URI uri, HttpMethod method) {
        if (isClosed()) return;

        try {
            var request = new XMLHttpRequest();
            request.setOnReadyStateChange(() -> onResponseStateChange(request));
            request.setResponseType("arraybuffer");
            var address = uri.toASCIIString();
            request.open(method.toString(), Main.CORS_PROXY.isEmpty() ? address : Main.CORS_PROXY.replace("{}", address));
            for (var iterator = headers.iteratorAsString(); iterator.hasNext(); ) {
                var header = iterator.next();
                request.setRequestHeader(header.getKey(), header.getValue());
            }
            request.setRequestHeader("X-CC-Redirect", followRedirects ? "true" : "false");
            request.send(postBuffer == null ? null : Int8Array.fromJavaBuffer(postBuffer));
            checkClosed();
        } catch (Exception e) {
            failure("Could not connect");
            LOG.error(Logging.HTTP_ERROR, "Error in HTTP request", e);
        }
    }

    private void onResponseStateChange(XMLHttpRequest request) {
        if (request.getReadyState() != XMLHttpRequest.DONE) return;
        if (request.getStatus() == 0) {
            this.failure("Could not connect");
            return;
        }

        var buffer = (ArrayBuffer) request.getResponse();
        SeekableByteChannel contents = new ArrayByteChannel(new Int8Array(buffer).copyToJavaArray());
        var reader = new ReadHandle(contents, binary);

        Map<String, String> responseHeaders = new HashMap<>();
        for (var header : Splitter.on("\r\n").split(request.getAllResponseHeaders())) {
            var index = header.indexOf(':');
            if (index < 0) continue;

            // Normalise the header (so "content-type" becomes "Content-Type")
            var upcase = true;
            var headerBuilder = new StringBuilder(index);
            for (var i = 0; i < index; i++) {
                var c = header.charAt(i);
                headerBuilder.append(upcase ? Character.toUpperCase(c) : c);
                upcase = c == '-';
            }
            responseHeaders.put(headerBuilder.toString(), header.substring(index + 1).trim());
        }
        var stream = new HttpResponseHandle(reader, request.getStatus(), request.getStatusText(), responseHeaders);

        if (request.getStatus() >= 200 && request.getStatus() < 400) {
            if (tryClose()) environment.queueEvent(SUCCESS_EVENT, address, stream);
        } else {
            if (tryClose()) environment.queueEvent(FAILURE_EVENT, address, request.getStatusText(), stream);
        }
    }

    void failure(String message) {
        if (tryClose()) environment.queueEvent(FAILURE_EVENT, address, message);
    }
}
