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
final class Websocket extends Resource<Websocket, Websocket.State> implements WebsocketClient {
    private final IAPIEnvironment environment;
    private final String address;

    Websocket(ResourceGroup<Websocket> limiter, IAPIEnvironment environment, String address, URI uri) {
        super(limiter, new Init(uri));
        this.environment = environment;
        this.address = address;
    }

    public sealed interface State extends ResourceState {
    }

    private record Init(URI uri) implements State {
        @Override
        public void close() {
        }
    }

    private record Connecting(WebSocket websocket) implements State {
        @Override
        public void close() {
            websocket.close();
        }
    }

    private sealed interface OpenWebsocketState extends State {
    }

    private record Connected(WebSocket websocket) implements OpenWebsocketState {
        @Override
        public void close() {
            websocket.close();
        }
    }

    private record Closing(WebSocket websocket) implements OpenWebsocketState {
        @Override
        public void close() {
            websocket.close();
        }
    }

    @Override
    public void start() {
        this.transformState(Init.class, s -> connect(s.uri));
    }

    private State connect(URI uri) {
        var client = new WebSocket(uri.toASCIIString());
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
        return new Connecting(client);
    }

    @Override
    public void sendText(String message) throws LuaException {
        try (var stateHolder = state()) {
            var state = stateHolder.get();
            if (state == null || state instanceof Closing) throw new LuaException(CLOSED_ERROR);
            if (!(state instanceof Connected connected)) throw new IllegalStateException("Unexpected state " + state);

            connected.websocket.send(message);
        }
    }

    @Override
    public void sendBinary(ByteBuffer message) throws LuaException {
        try (var stateHolder = state()) {
            var state = stateHolder.get();
            if (state == null || state instanceof Closing) throw new LuaException(CLOSED_ERROR);
            if (!(state instanceof Connected connected)) throw new IllegalStateException("Unexpected state " + state);

            connected.websocket.send(Int8Array.fromJavaBuffer(message));
        }
    }

    @Override
    public void close(int status, String reason) {
        try (var stateHolder = state()) {
            var state = stateHolder.get();
            if (state == null || state instanceof Closing) return;
            if (!(state instanceof Connected connected)) throw new IllegalStateException("Unexpected state " + state);

            connected.websocket.close(status, reason);
        }
    }

    private void success(Options options) {
        transformState(Connecting.class, s -> {
            var handle = new WebsocketHandle(environment, address, this, Map.of(), options);
            environment.queueEvent(SUCCESS_EVENT, address, handle);
            GlobalCleaner.register(handle, () -> close(1001, ""));
            return new Connected(s.websocket);
        });
    }

    private void handshakeFailure(String message) {
        close(Connecting.class, () -> environment.queueEvent(FAILURE_EVENT, address, message));
    }

    private void serverClosed(int status, String reason) {
        close(OpenWebsocketState.class, () -> environment.queueEvent(
            CLOSE_EVENT, address, Strings.isNullOrEmpty(reason) ? null : reason, status < 0 ? null : status
        ));
    }
}
