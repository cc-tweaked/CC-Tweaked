// SPDX-FileCopyrightText: 2023 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package cc.tweaked.web.http;

import cc.tweaked.web.js.Console;
import com.google.common.base.Strings;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.core.apis.IAPIEnvironment;
import dan200.computercraft.core.apis.http.Resource;
import dan200.computercraft.core.apis.http.ResourceGroup;
import dan200.computercraft.core.apis.http.options.Action;
import dan200.computercraft.core.apis.http.options.Options;
import dan200.computercraft.core.apis.http.websocket.WebsocketClient;
import dan200.computercraft.core.apis.http.websocket.WebsocketHandle;
import dan200.computercraft.core.util.GlobalCleaner;
import org.jspecify.annotations.Nullable;
import org.teavm.jso.typedarrays.ArrayBuffer;
import org.teavm.jso.typedarrays.Int8Array;
import org.teavm.jso.websocket.WebSocket;

import java.net.URI;
import java.nio.ByteBuffer;
import java.util.Map;

/**
 * Equivalent to {@link dan200.computercraft.core.apis.http.websocket.Websocket}, but using Javascript's built-in
 * {@link WebSocket} client.
 */
final class Websocket extends Resource<Websocket> implements WebsocketClient {
    private final IAPIEnvironment environment;
    private final URI uri;
    private final String address;

    private @Nullable WebSocket websocket;
    private boolean isClosing = false;

    Websocket(ResourceGroup<Websocket> limiter, IAPIEnvironment environment, String address, URI uri) {
        super(limiter);
        this.environment = environment;
        this.uri = uri;
        this.address = address;
    }

    public void connect() {
        if (isClosed()) return;

        var client = this.websocket = new WebSocket(uri.toASCIIString());
        client.setBinaryType("arraybuffer");
        client.onOpen(e -> success(Action.ALLOW.toPartial().toOptions()));
        client.onError(e -> {
            Console.error(e);
            handshakeFailure("Could not connect");
        });
        client.onMessage(e -> {
            if (isClosed()) return;
            if (e.getData() instanceof ArrayBuffer buffer) {
                var contents = new Int8Array(buffer).copyToJavaArray();
                environment.queueEvent("websocket_message", address, contents, true);
            } else {
                environment.queueEvent("websocket_message", address, e.getDataAsString(), false);
            }
        });
        client.onClose(e -> serverClosed(e.getCode(), e.getReason()));
    }

    @Override
    public void sendText(String message) throws LuaException {
        if (websocket == null || isClosing) throw new LuaException(CLOSED_ERROR);
        websocket.send(message);
    }

    @Override
    public void sendBinary(ByteBuffer message) throws LuaException {
        if (websocket == null || isClosing) throw new LuaException(CLOSED_ERROR);
        websocket.send(Int8Array.fromJavaBuffer(message));
    }

    @Override
    public void close(int status, String reason) {
        if (websocket == null || isClosing) return;

        websocket.close(status, reason);
        isClosing = true;
    }

    @Override
    protected void dispose() {
        super.dispose();
        if (websocket != null) {
            websocket.close();
            websocket = null;
        }
    }

    private void success(Options options) {
        if (isClosed()) return;

        var handle = new WebsocketHandle(environment, address, this, Map.of(), options);
        environment.queueEvent(SUCCESS_EVENT, address, handle);
        GlobalCleaner.register(handle, () -> close(1001, ""));

        checkClosed();
    }

    private void handshakeFailure(String message) {
        if (tryClose()) environment.queueEvent(FAILURE_EVENT, address, message);
    }

    private void serverClosed(int status, String reason) {
        if (!tryClose()) return;

        environment.queueEvent(CLOSE_EVENT, address, Strings.isNullOrEmpty(reason) ? null : reason, status < 0 ? null : status);
    }
}
