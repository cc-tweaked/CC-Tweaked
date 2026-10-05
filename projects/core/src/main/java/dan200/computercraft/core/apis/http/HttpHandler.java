// SPDX-FileCopyrightText: 2026 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.core.apis.http;

import dan200.computercraft.core.apis.IAPIEnvironment;
import dan200.computercraft.core.apis.handles.AbstractHandle;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import org.jspecify.annotations.Nullable;

import java.net.URI;
import java.nio.ByteBuffer;

/**
 * Handles HTTP requests for a computer.
 */
public interface HttpHandler {
    int DEFAULT_MAX_REQUESTS = 16;
    int DEFAULT_MAX_WEBSOCKETS = 4;

    /**
     * Start up this handler. This should be called before any requests occur.
     */
    void startup();

    /**
     * Shut down this handler, stopping any in-flight requests.
     */
    void shutdown();

    /**
     * Queue a task to check whether a URL is valid. This should queue a {@code "http_check"} event on completion.
     *
     * @param address The address to check.
     * @param uri     The URI equivalent of the address.
     * @return Whether the task could be queued. Should return false if there are too many in-flight checks.
     */
    boolean queueCheckUrl(String address, URI uri);

    /**
     * Queue a task to make a HTTP request. This should queue a {@code "http_success"} or {@code "http_failure"} event
     * on completion.
     *
     * @param address         The address to request.
     * @param uri             The URI equivalent of the address.
     * @param method          The HTTP method to use for this request.
     * @param postBody        The body of this request. Should be {@code null} if making a {@code GET} or
     *                        {@code CONNECT} request.
     * @param headers         The headers of this request.
     * @param binary          Whether the returned HTTP handle should use binary mode. See {@link AbstractHandle} for
     *                        more details.
     * @param followRedirects Whether to follow redirects.
     * @param timeout         The timeout for this request.
     * @return Whether the task could be queued. Should return false if there are too many in-flight requests.
     */
    boolean queueRequest(
        String address, URI uri, HttpMethod method, @Nullable ByteBuffer postBody, HttpHeaders headers, boolean binary, boolean followRedirects, int timeout
    );

    /**
     * Queue a task to open a websocket. This should queue a {@code "websocket_success"} or {@code "websocket_failure"}
     * once the websocket is opened.
     *
     * @param address The address to connect to.
     * @param uri     The URI equivalent of the address.
     * @param headers The headers of this request.
     * @param timeout The timeout for establishing a connection.
     * @return Whether the task could be queued. Should return false if there are too many in-flight websockets.
     */
    boolean queueWebsocket(String address, URI uri, HttpHeaders headers, int timeout);

    /**
     * A factory for a {@link HttpHandler}.
     */
    interface Factory {
        /**
         * Create a new {@link HttpHandler} for a computer.
         *
         * @param environment The computer's API environment.
         * @return The HTTP provider, or {@code null} if HTTP should be disabled on this computer.
         */
        @Nullable HttpHandler create(IAPIEnvironment environment);
    }
}
