// SPDX-FileCopyrightText: 2023 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.core.apis.http.websocket;

import cc.tweaked.web.js.Console;
import com.google.common.base.Strings;
import dan200.computercraft.core.apis.IAPIEnvironment;
import dan200.computercraft.core.apis.http.Resource;
import dan200.computercraft.core.apis.http.ResourceGroup;
import dan200.computercraft.core.apis.http.options.Action;
import dan200.computercraft.core.apis.http.options.Options;
import io.netty.handler.codec.http.HttpHeaders;
import org.jspecify.annotations.Nullable;
import org.teavm.jso.typedarrays.ArrayBuffer;
import org.teavm.jso.typedarrays.Int8Array;
import org.teavm.jso.websocket.WebSocket;

import java.net.URI;
import java.nio.ByteBuffer;
import java.util.Map;

/**
 * Replaces {@link Websocket} with a version which uses Javascript's built-in {@link WebSocket} client.
 */
public class TWebsocket extends Resource<TWebsocket> implements WebsocketClient {
    private final IAPIEnvironment environment;
    private final URI uri;
    private final String address;

    private @Nullable WebSocket websocket;

    public TWebsocket(ResourceGroup<TWebsocket> limiter, IAPIEnvironment environment, URI uri, String address, HttpHeaders headers, int timeout) {
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
            failure("Could not connect");
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
        client.onClose(e -> close(e.getCode(), e.getReason()));
    }

    @Override
    public void sendText(String message) {
        if (websocket == null) return;
        websocket.send(message);
    }

    @Override
    public void sendBinary(ByteBuffer message) {
        if (websocket == null) return;
        websocket.send(Int8Array.fromJavaBuffer(message));
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
        createOwnerReference(handle);

        checkClosed();
    }

    void failure(String message) {
        if (tryClose()) environment.queueEvent(FAILURE_EVENT, address, message);
    }

    void close(int status, String reason) {
        if (!tryClose()) return;

        environment.queueEvent(CLOSE_EVENT, address, Strings.isNullOrEmpty(reason) ? null : reason, status < 0 ? null : status);
    }
}
