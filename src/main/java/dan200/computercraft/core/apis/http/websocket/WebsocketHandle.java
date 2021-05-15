/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.core.apis.http.websocket;

import static dan200.computercraft.api.lua.LuaValues.checkFinite;
import static dan200.computercraft.core.apis.IAPIEnvironment.TIMER_EVENT;
import static dan200.computercraft.core.apis.http.websocket.Websocket.CLOSE_EVENT;
import static dan200.computercraft.core.apis.http.websocket.Websocket.MESSAGE_EVENT;

import java.io.Closeable;
import java.util.Arrays;
import java.util.Optional;

import javax.annotation.Nonnull;

import com.google.common.base.Objects;
import dan200.computercraft.api.lua.ILuaCallback;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.lua.LuaValues;
import dan200.computercraft.api.lua.MethodResult;
import dan200.computercraft.core.apis.http.options.Options;
import dan200.computercraft.core.tracking.TrackingField;
import dan200.computercraft.shared.util.StringUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;

/**
 * A websocket, which can be used to send an receive messages with a web server.
 *
 * @cc.module http.Websocket
 * @see dan200.computercraft.core.apis.HTTPAPI#websocket On how to open a websocket.
 */
public class WebsocketHandle implements Closeable {
    private final Websocket websocket;
    private final Options options;
    private boolean closed = false;

    private Channel channel;

    public WebsocketHandle(Websocket websocket, Options options, Channel channel) {
        this.websocket = websocket;
        this.options = options;
        this.channel = channel;
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
     */
    @LuaFunction
    public final MethodResult receive(Optional<Double> timeout) throws LuaException {
        this.checkOpen();
        int timeoutId = timeout.isPresent() ? this.websocket.environment()
                                                            .startTimer(Math.round(checkFinite(0, timeout.get()) / 0.05)) : -1;

        return new ReceiveCallback(timeoutId).pull;
    }

    private void checkOpen() throws LuaException {
        if (this.closed) {
            throw new LuaException("attempt to use a closed file");
        }
    }

    /**
     * Send a websocket message to the connected server.
     *
     * @param message The message to send.
     * @param binary Whether this message should be treated as a
     * @throws LuaException If the message is too large.
     * @throws LuaException If the websocket has been closed.
     */
    @LuaFunction
    public final void send(Object message, Optional<Boolean> binary) throws LuaException {
        this.checkOpen();

        String text = StringUtil.toString(message);
        if (this.options.websocketMessage != 0 && text.length() > this.options.websocketMessage) {
            throw new LuaException("Message is too large");
        }

        this.websocket.environment()
                      .addTrackingChange(TrackingField.WEBSOCKET_OUTGOING, text.length());

        Channel channel = this.channel;
        if (channel != null) {
            channel.writeAndFlush(binary.orElse(false) ? new BinaryWebSocketFrame(Unpooled.wrappedBuffer(LuaValues.encode(text))) : new TextWebSocketFrame(
                text));
        }
    }

    /**
     * Close this websocket. This will terminate the connection, meaning messages can no longer be sent or received along it.
     */
    @LuaFunction ("close")
    public final void doClose() {
        this.close();
        this.websocket.close();
    }

    @Override
    public void close() {
        this.closed = true;

        Channel channel = this.channel;
        if (channel != null) {
            channel.close();
            this.channel = null;
        }
    }

    private final class ReceiveCallback implements ILuaCallback {
        final MethodResult pull = MethodResult.pullEvent(null, this);
        private final int timeoutId;

        ReceiveCallback(int timeoutId) {
            this.timeoutId = timeoutId;
        }

        @Nonnull
        @Override
        public MethodResult resume(Object[] event) {
            if (event.length >= 3 && Objects.equal(event[0], MESSAGE_EVENT) && Objects.equal(event[1], WebsocketHandle.this.websocket.address())) {
                return MethodResult.of(Arrays.copyOfRange(event, 2, event.length));
            } else if (event.length >= 2 && Objects.equal(event[0], CLOSE_EVENT) && Objects.equal(event[1], WebsocketHandle.this.websocket.address()) && WebsocketHandle.this.closed) {
                // If the socket is closed abort.
                return MethodResult.of();
            } else if (event.length >= 2 && this.timeoutId != -1 && Objects.equal(event[0],
                                                                                  TIMER_EVENT) && event[1] instanceof Number && ((Number) event[1]).intValue() == this.timeoutId) {
                // If we received a matching timer event then abort.
                return MethodResult.of();
            }

            return this.pull;
        }
    }
}
