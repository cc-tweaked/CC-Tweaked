// SPDX-FileCopyrightText: 2019 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.core.apis.http.websocket;

import dan200.computercraft.api.lua.*;
import dan200.computercraft.core.apis.IAPIEnvironment;
import dan200.computercraft.core.apis.http.options.Options;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

import static dan200.computercraft.api.lua.LuaValues.checkFinite;
import static dan200.computercraft.core.apis.IAPIEnvironment.TIMER_EVENT;
import static dan200.computercraft.core.apis.http.websocket.WebsocketClient.CLOSE_EVENT;
import static dan200.computercraft.core.apis.http.websocket.WebsocketClient.MESSAGE_EVENT;

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

    /**
     * Wait for a message from the server.
     *
     * @param timeout The number of seconds to wait if no message is received.
     * @return The result of receiving.
     * @throws LuaException If the websocket has been closed.
     * @cc.treturn [1] string The received message.
     * @cc.treturn boolean If this was a binary message.
     * @cc.treturn [2] nil If the websocket was closed while waiting, or if we timed out.
     * @cc.changed 1.80pr1.13 Added return value indicating whether the message was binary.
     * @cc.changed 1.87.0 Added timeout argument.
     */
    @LuaFunction
    public final MethodResult receive(Optional<Double> timeout) throws LuaException {
        checkOpen();
        var timeoutId = timeout.isPresent()
            ? environment.startTimer(Math.round(checkFinite(0, timeout.get()) / 0.05))
            : -1;

        return new ReceiveCallback(timeoutId).pull;
    }

    /**
     * Send a websocket message to the connected server.
     *
     * @param message The message to send.
     * @param binary  Whether this message should be treated as a
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

    private final class ReceiveCallback implements ILuaCallback {
        final MethodResult pull = MethodResult.pullEvent(null, this);
        private final int timeoutId;

        ReceiveCallback(int timeoutId) {
            this.timeoutId = timeoutId;
        }

        @Override
        public MethodResult resume(Object[] event) {
            if (event.length >= 3 && Objects.equals(event[0], MESSAGE_EVENT) && Objects.equals(event[1], address)) {
                return MethodResult.of(Arrays.copyOfRange(event, 2, event.length));
            } else if (event.length >= 2 && Objects.equals(event[0], CLOSE_EVENT) && Objects.equals(event[1], address) && websocket.isClosed()) {
                // If the socket is closed abort.
                return MethodResult.of();
            } else if (event.length >= 2 && timeoutId != -1 && Objects.equals(event[0], TIMER_EVENT)
                && event[1] instanceof Number id && id.intValue() == timeoutId) {
                // If we received a matching timer event then abort.
                return MethodResult.of();
            }

            return pull;
        }
    }
}
