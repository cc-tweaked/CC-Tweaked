// SPDX-FileCopyrightText: 2023 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.core.apis.http.websocket;

import dan200.computercraft.api.lua.LuaException;
import io.netty.handler.codec.http.websocketx.WebSocketCloseStatus;

import java.nio.ByteBuffer;

/**
 * A client-side websocket, which can be used to send messages to a remote server.
 * <p>
 * {@link WebsocketHandle} wraps this into a Lua-compatible interface.
 */
public interface WebsocketClient {
    String SUCCESS_EVENT = "websocket_success";
    String FAILURE_EVENT = "websocket_failure";
    String CLOSE_EVENT = "websocket_closed";
    String MESSAGE_EVENT = "websocket_message";

    /**
     * Error message to throw when attempting to use a {@linkplain #isClosed() closed} websocket.
     */
    String CLOSED_ERROR = "attempt to use a closed websocket";

    /**
     * Determine whether this websocket is closed.
     *
     * @return Whether this websocket is closed.
     */
    boolean isClosed();

    /**
     * Close this websocket.
     *
     * @param status The status code for this closure. Must be a {@linkplain WebSocketCloseStatus#isValidStatusCode(int)
     *               valid status code}.
     * @param reason The reason for this closure.
     * @see WebSocketCloseStatus
     */
    void close(int status, String reason);

    /**
     * Send a text websocket frame.
     *
     * @param message The message to send.
     * @throws LuaException If the message could not be sent.
     */
    void sendText(String message) throws LuaException;

    /**
     * Send a binary websocket frame.
     *
     * @param message The message to send.
     * @throws LuaException If the message could not be sent.
     */
    void sendBinary(ByteBuffer message) throws LuaException;
}
