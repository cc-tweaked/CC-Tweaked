// SPDX-FileCopyrightText: 2023 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.core.apis.http.websocket;

import cc.tweaked.web.js.Console;
import cc.tweaked.web.js.JavascriptConv;
import com.google.common.base.Strings;
import dan200.computercraft.core.apis.IAPIEnvironment;
import dan200.computercraft.core.apis.http.Resource;
import dan200.computercraft.core.apis.http.ResourceGroup;
import dan200.computercraft.core.apis.http.options.Action;
import dan200.computercraft.core.apis.http.options.Options;
import io.netty.handler.codec.http.HttpHeaders;
import org.teavm.jso.typedarrays.Int8Array;
import org.teavm.jso.websocket.WebSocket;

import javax.annotation.Nullable;
import java.net.URI;
import java.nio.ByteBuffer;

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
            if (JavascriptConv.isArrayBuffer(e.getData())) {
                var array = new Int8Array(e.getDataAsArray());
                var contents = new byte[array.getLength()];
                for (var i = 0; i < contents.length; i++) contents[i] = array.get(i);
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
        websocket.send(JavascriptConv.toArray(message));
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

        var handle = new WebsocketHandle(environment, address, this, options);
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
