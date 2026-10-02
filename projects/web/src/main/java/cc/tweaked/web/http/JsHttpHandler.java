// SPDX-FileCopyrightText: 2026 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package cc.tweaked.web.http;

import dan200.computercraft.core.apis.IAPIEnvironment;
import dan200.computercraft.core.apis.http.HttpHandler;
import dan200.computercraft.core.apis.http.ResourceGroup;
import dan200.computercraft.core.apis.http.ResourceQueue;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import org.jspecify.annotations.Nullable;

import java.net.URI;
import java.nio.ByteBuffer;

public final class JsHttpHandler implements HttpHandler {
    private final IAPIEnvironment environment;

    private final ResourceGroup<HttpRequest> requests = new ResourceQueue<>(() -> HttpHandler.DEFAULT_MAX_REQUESTS);
    private final ResourceGroup<Websocket> websockets = new ResourceGroup<>(() -> HttpHandler.DEFAULT_MAX_WEBSOCKETS);

    public JsHttpHandler(IAPIEnvironment environment) {
        this.environment = environment;
    }

    @Override
    public void startup() {
        requests.startup();
        websockets.startup();
    }

    @Override
    public void shutdown() {
        requests.shutdown();
        websockets.shutdown();
    }

    @Override
    public boolean queueCheckUrl(String address, URI uri) {
        environment.queueEvent("http_check", address, true);
        return true;
    }

    @Override
    public boolean queueRequest(String address, URI uri, HttpMethod method, @Nullable ByteBuffer postBody, HttpHeaders headers, boolean binary, boolean followRedirects, int timeout, boolean streaming) {
        return new HttpRequest(requests, environment, address, postBody, headers, binary, followRedirects, streaming).queue(r -> r.request(uri, method));
    }

    @Override
    public boolean queueWebsocket(String address, URI uri, HttpHeaders headers, int timeout) {
        return new Websocket(websockets, environment, address, uri).queue(Websocket::connect);
    }
}
