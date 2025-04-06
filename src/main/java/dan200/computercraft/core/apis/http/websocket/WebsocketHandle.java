// SPDX-FileCopyrightText: 2019 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.core.apis.http.websocket;

import dan200.computer.core.IAPIEnvironment;
import dan200.computercraft.api.lua.Coerced;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.core.apis.http.options.Options;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

/**
 * A websocket, which can be used to send and receive messages with a web server.
 *
 * @cc.module http.Websocket
 * @see dan200.computercraft.core.apis.HTTPAPI#websocket On how to open a websocket.
 */
public class WebsocketHandle {
    private static final ThreadLocal<CharsetDecoder> DECODER = ThreadLocal.withInitial(() -> StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPLACE));

    private final IAPIEnvironment environment;
    private final String address;
    private final WebsocketClient websocket;
    private final Options options;

    public WebsocketHandle(IAPIEnvironment environment, String address, WebsocketClient websocket, Options options) {
        this.environment = environment;
        this.address = address;
        this.websocket = websocket;
        this.options = options;
    }

    // TODO: Can we implement receive()?

    /**
     * Send a websocket message to the connected server.
     *
     * @param message The message to send.
     * @param binary  Whether this message should be treated as a binary message.
     * @throws LuaException If the message is too large.
     * @throws LuaException If the websocket has been closed.
     * @cc.changed 1.81.0 Added argument for binary mode.
     */
    @LuaFunction
    public final void send(Coerced<ByteBuffer> message, Optional<Boolean> binary) throws LuaException {
        checkOpen();

        var text = message.value();
        if (options.websocketMessage() != 0 && text.remaining() > options.websocketMessage()) {
            throw new LuaException("Message is too large");
        }

        if (binary.orElse(false)) {
            websocket.sendBinary(text);
        } else {
            try {
                websocket.sendText(DECODER.get().decode(text).toString());
            } catch (CharacterCodingException e) {
                // This shouldn't happen, but worth mentioning.
                throw new LuaException("Message is not valid UTF8");
            }
        }
    }

    /**
     * Close this websocket. This will terminate the connection, meaning messages can no longer be sent or received
     * along it.
     */
    @LuaFunction
    public final void close() {
        websocket.close();
    }

    private void checkOpen() throws LuaException {
        if (websocket.isClosed()) throw new LuaException("attempt to use a closed file");
    }
}
