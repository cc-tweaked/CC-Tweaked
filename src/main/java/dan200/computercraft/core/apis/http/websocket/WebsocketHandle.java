/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.core.apis.http.websocket;

import static dan200.computercraft.core.apis.ArgumentHelper.optBoolean;
import static dan200.computercraft.core.apis.http.websocket.Websocket.CLOSE_EVENT;
import static dan200.computercraft.core.apis.http.websocket.Websocket.MESSAGE_EVENT;

import java.io.Closeable;
import java.util.Arrays;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.google.common.base.Objects;
import dan200.computercraft.ComputerCraft;
import dan200.computercraft.api.lua.ILuaContext;
import dan200.computercraft.api.lua.ILuaObject;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.core.tracking.TrackingField;
import dan200.computercraft.shared.util.StringUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;

public class WebsocketHandle implements ILuaObject, Closeable {
    private final Websocket websocket;
    private boolean closed = false;

    private Channel channel;

    public WebsocketHandle(Websocket websocket, Channel channel) {
        this.websocket = websocket;
        this.channel = channel;
    }

    @Nonnull
    @Override
    public String[] getMethodNames() {
        return new String[] {
            "receive",
            "send",
            "close"
        };
    }

    @Nullable
    @Override
    public Object[] callMethod(@Nonnull ILuaContext context, int method, @Nonnull Object[] arguments) throws LuaException, InterruptedException {
        switch (method) {
        case 0: // receive
            this.checkOpen();
            while (true) {
                Object[] event = context.pullEvent(null);
                if (event.length >= 3 && Objects.equal(event[0], MESSAGE_EVENT) && Objects.equal(event[1], this.websocket.address())) {
                    return Arrays.copyOfRange(event, 2, event.length);
                } else if (event.length >= 2 && Objects.equal(event[0], CLOSE_EVENT) && Objects.equal(event[1], this.websocket.address()) && this.closed) {
                    return null;
                }
            }

        case 1: // send
        {
            this.checkOpen();

            String text = arguments.length > 0 && arguments[0] != null ? arguments[0].toString() : "";
            if (ComputerCraft.httpMaxWebsocketMessage != 0 && text.length() > ComputerCraft.httpMaxWebsocketMessage) {
                throw new LuaException("Message is too large");
            }

            boolean binary = optBoolean(arguments, 1, false);
            this.websocket.environment()
                          .addTrackingChange(TrackingField.WEBSOCKET_OUTGOING, text.length());

            Channel channel = this.channel;
            if (channel != null) {
                channel.writeAndFlush(binary ? new BinaryWebSocketFrame(Unpooled.wrappedBuffer(StringUtil.encodeString(text))) :
                                      new TextWebSocketFrame(text));
            }

            return null;
        }

        case 2: // close
            this.close();
            this.websocket.close();
            return null;
        default:
            return null;
        }
    }

    private void checkOpen() throws LuaException {
        if (this.closed) {
            throw new LuaException("attempt to use a closed file");
        }
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
}
