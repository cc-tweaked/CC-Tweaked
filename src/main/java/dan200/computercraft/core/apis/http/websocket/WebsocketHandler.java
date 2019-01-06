/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.core.apis.http.websocket;

import com.google.common.base.Objects;
import dan200.computercraft.api.lua.ILuaContext;
import dan200.computercraft.api.lua.ILuaObject;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.core.apis.http.NetworkUtils;
import dan200.computercraft.core.tracking.TrackingField;
import dan200.computercraft.shared.util.StringUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.websocketx.*;
import io.netty.util.CharsetUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.Closeable;
import java.util.Arrays;

import static dan200.computercraft.core.apis.ArgumentHelper.optBoolean;
import static dan200.computercraft.core.apis.http.websocket.Websocket.MESSAGE_EVENT;
import static dan200.computercraft.core.apis.http.websocket.Websocket.SUCCESS_EVENT;

public class WebsocketHandler extends SimpleChannelInboundHandler<Object> implements ILuaObject, Closeable
{
    private final Websocket parent;
    private boolean closed = false;

    private final String url;
    private final WebSocketClientHandshaker handshaker;

    private Channel channel;

    public WebsocketHandler( Websocket parent, WebSocketClientHandshaker handshaker, String url )
    {
        this.handshaker = handshaker;
        this.url = url;
        this.parent = parent;
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

    private void onClosed( int status, String reason )
    {
        parent.close( status, reason );
        close();
    }

    @Override
    public void handlerAdded( ChannelHandlerContext ctx ) throws Exception
    {
        channel = ctx.channel();
        super.handlerAdded( ctx );
    }

    @Override
    public void channelActive( ChannelHandlerContext ctx ) throws Exception
    {
        handshaker.handshake( ctx.channel() );
        super.channelActive( ctx );
    }

    @Override
    public void channelInactive( ChannelHandlerContext ctx ) throws Exception
    {
        if( !closed ) onClosed( -1, "Websocket is inactive" );
        super.channelInactive( ctx );
    }

    @Override
    public void channelRead0( ChannelHandlerContext ctx, Object msg )
    {
        if( parent.checkClosed() ) return;

        if( !handshaker.isHandshakeComplete() )
        {
            handshaker.finishHandshake( ctx.channel(), (FullHttpResponse) msg );
            parent.environment().queueEvent( SUCCESS_EVENT, new Object[] { url, this } );
            return;
        }

        if( msg instanceof FullHttpResponse )
        {
            FullHttpResponse response = (FullHttpResponse) msg;
            throw new IllegalStateException( "Unexpected FullHttpResponse (getStatus=" + response.status() + ", content=" + response.content().toString( CharsetUtil.UTF_8 ) + ')' );
        }

        WebSocketFrame frame = (WebSocketFrame) msg;
        if( frame instanceof TextWebSocketFrame )
        {
            String data = ((TextWebSocketFrame) frame).text();

            parent.environment().addTrackingChange( TrackingField.WEBSOCKET_INCOMING, data.length() );
            parent.environment().queueEvent( MESSAGE_EVENT, new Object[] { url, data, false } );
        }
        else if( frame instanceof BinaryWebSocketFrame )
        {
            byte[] converted = NetworkUtils.toBytes( frame.content() );

            parent.environment().addTrackingChange( TrackingField.WEBSOCKET_INCOMING, converted.length );
            parent.environment().queueEvent( MESSAGE_EVENT, new Object[] { url, converted, true } );
        }
        else if( frame instanceof CloseWebSocketFrame )
        {
            CloseWebSocketFrame closeFrame = (CloseWebSocketFrame) frame;
            onClosed( closeFrame.statusCode(), closeFrame.reasonText() );
        }
    }

    @Override
    public void exceptionCaught( ChannelHandlerContext ctx, Throwable cause )
    {
        ctx.close();
        parent.failure( cause instanceof WebSocketHandshakeException ? cause.getMessage() : "Could not connect" );
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
                    if( event.length >= 3 && Objects.equal( event[1], url ) )
                    {
                        return Arrays.copyOfRange( event, 2, event.length );
                    }
                }

            case 1: // send
            {
                checkOpen();
                String text = arguments.length > 0 && arguments[0] != null ? arguments[0].toString() : "";
                boolean binary = optBoolean( arguments, 1, false );
                parent.environment().addTrackingChange( TrackingField.WEBSOCKET_OUTGOING, text.length() );

                Channel channel = this.channel;
                if( channel != null )
                {
                    channel.writeAndFlush( binary
                        ? new BinaryWebSocketFrame( Unpooled.wrappedBuffer( StringUtil.encodeString( text ) ) )
                        : new TextWebSocketFrame( text ) );
                }

                return null;
            }
            case 2:
                parent.close();
                close();
                return null;
            default:
                return null;
        }
    }

    private void checkOpen() throws LuaException
    {
        if( closed ) throw new LuaException( "attempt to use a closed file" );
    }
}
