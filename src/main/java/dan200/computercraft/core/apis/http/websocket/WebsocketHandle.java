/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.core.apis.http.websocket;

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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.Closeable;
import java.util.Arrays;

import static dan200.computercraft.core.apis.ArgumentHelper.optBoolean;
import static dan200.computercraft.core.apis.http.websocket.Websocket.MESSAGE_EVENT;

public class WebsocketHandle implements ILuaObject, Closeable
{
    private final Websocket websocket;
    private boolean closed = false;

    private Channel channel;

    public WebsocketHandle( Websocket websocket, Channel channel )
    {
        this.websocket = websocket;
        this.channel = channel;
    }

    @Nonnull
    @Override
    public String[] getMethodNames()
    {
        return new String[] { "receive", "send", "close" };
    }

    @Nullable
    @Override
    public Object[] callMethod( @Nonnull ILuaContext context, int method, @Nonnull Object[] arguments ) throws LuaException, InterruptedException
    {
        switch( method )
        {
            case 0: // receive
                while( true )
                {
                    checkOpen();

                    Object[] event = context.pullEvent( MESSAGE_EVENT );
                    if( event.length >= 3 && Objects.equal( event[1], websocket.address() ) )
                    {
                        return Arrays.copyOfRange( event, 2, event.length );
                    }
                }

            case 1: // send
            {
                checkOpen();

                String text = arguments.length > 0 && arguments[0] != null ? arguments[0].toString() : "";
                if( ComputerCraft.httpMaxWebsocketMessage != 0 && text.length() > ComputerCraft.httpMaxWebsocketMessage )
                {
                    throw new LuaException( "Message is too large" );
                }

                boolean binary = optBoolean( arguments, 1, false );
                websocket.environment().addTrackingChange( TrackingField.WEBSOCKET_OUTGOING, text.length() );

                Channel channel = this.channel;
                if( channel != null )
                {
                    channel.writeAndFlush( binary
                        ? new BinaryWebSocketFrame( Unpooled.wrappedBuffer( StringUtil.encodeString( text ) ) )
                        : new TextWebSocketFrame( text ) );
                }

                return null;
            }

            case 2: // close
                close();
                websocket.close();
                return null;
            default:
                return null;
        }
    }

    private void checkOpen() throws LuaException
    {
        if( closed ) throw new LuaException( "attempt to use a closed file" );
    }

    @Override
    public void close()
    {
        closed = true;

        Channel channel = this.channel;
        if( channel != null )
        {
            channel.close();
            this.channel = null;
        }
    }
}
