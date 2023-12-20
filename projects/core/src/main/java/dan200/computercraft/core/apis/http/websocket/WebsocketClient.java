// SPDX-FileCopyrightText: 2023 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.core.apis.http.websocket;

import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.core.apis.http.HTTPRequestException;

import java.io.Closeable;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;

/**
 * A client-side websocket, which can be used to send messages to a remote server.
 * <p>
 * {@link WebsocketHandle} wraps this into a Lua-compatible interface.
 */
public interface WebsocketClient extends Closeable {
    String SUCCESS_EVENT = "websocket_success";
    String FAILURE_EVENT = "websocket_failure";
    String CLOSE_EVENT = "websocket_closed";
    String MESSAGE_EVENT = "websocket_message";

    /**
     * Determine whether this websocket is closed.
     *
     * @return Whether this websocket is closed.
     */
    boolean isClosed();

    /**
     * Close this websocket.
     */
    @Override
    void close();

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

    /**
     * Parse an address, ensuring it is a valid websocket URI.
     *
     * @param address The address to parse.
     * @return The parsed URI.
     * @throws HTTPRequestException If the address is not valid.
     */
    static URI parseUri(String address) throws HTTPRequestException {
        URI uri = null;
        try {
            uri = new URI(address);
        } catch (URISyntaxException ignored) {
            // Fall through to the case below
        }

        if (uri == null || uri.getHost() == null) {
            try {
                uri = new URI("ws://" + address);
            } catch (URISyntaxException ignored) {
                // Fall through to the case below
            }
        }

        if (uri == null || uri.getHost() == null) throw new HTTPRequestException("URL malformed");

        var scheme = uri.getScheme();
        if (scheme == null) {
            try {
                uri = new URI("ws://" + uri);
            } catch (URISyntaxException e) {
                throw new HTTPRequestException("URL malformed");
            }
        } else if (!scheme.equalsIgnoreCase("wss") && !scheme.equalsIgnoreCase("ws")) {
            throw new HTTPRequestException("Invalid scheme '" + scheme + "'");
        }

        return uri;
    }
}
