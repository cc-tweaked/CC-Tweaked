/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2017. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.core.apis.http;

import com.google.common.base.Objects;
import dan200.computercraft.api.lua.ILuaContext;
import dan200.computercraft.api.lua.ILuaObject;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.core.apis.HTTPAPI;
import dan200.computercraft.core.apis.IAPIEnvironment;
import dan200.computercraft.core.tracking.TrackingField;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.websocketx.*;
import io.netty.util.CharsetUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.Closeable;
import java.io.IOException;

public class WebsocketConnection extends SimpleChannelInboundHandler<Object> implements ILuaObject, Closeable
{
    public static final String SUCCESS_EVENT = "websocket_success";
    public static final String FAILURE_EVENT = "websocket_failure";
    public static final String CLOSE_EVENT = "websocket_closed";
    public static final String MESSAGE_EVENT = "websocket_message";

    private final String url;
    private final IAPIEnvironment computer;
    private final HTTPAPI api;
    private boolean open = true;

    private Channel channel;
    private final WebSocketClientHandshaker handshaker;

    public WebsocketConnection( IAPIEnvironment computer, HTTPAPI api, WebSocketClientHandshaker handshaker, String url )
    {
        this.computer = computer;
        this.api = api;
        this.handshaker = handshaker;
        this.url = url;

        api.addCloseable( this );
    }

    private void close( boolean remove )
    {
        open = false;
        if( remove ) api.removeCloseable( this );

        if( channel != null )
        {
            channel.close();
            channel = null;
        }
    }

    @Override
    public void close() throws IOException
    {
        close( false );
    }

    private void onClosed()
    {
        close( true );
        computer.queueEvent( CLOSE_EVENT, new Object[]{ url } );
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
        onClosed();
        super.channelInactive( ctx );
    }

    @Override
    public void channelRead0( ChannelHandlerContext ctx, Object msg ) throws Exception
    {
        Channel ch = ctx.channel();
        if( !handshaker.isHandshakeComplete() )
        {
            handshaker.finishHandshake( ch, (FullHttpResponse) msg );
            computer.queueEvent( SUCCESS_EVENT, new Object[]{ url, this } );
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

            computer.addTrackingChange( TrackingField.WEBSOCKET_INCOMING, data.length() );
            computer.queueEvent( MESSAGE_EVENT, new Object[]{ url, data } );
        }
        else if( frame instanceof BinaryWebSocketFrame )
        {
            ByteBuf data = frame.content();
            byte[] converted = new byte[data.readableBytes()];
            data.readBytes( converted );

            computer.addTrackingChange( TrackingField.WEBSOCKET_INCOMING, converted.length );
            computer.queueEvent( MESSAGE_EVENT, new Object[]{ url, converted } );
        }
        else if( frame instanceof CloseWebSocketFrame )
        {
            ch.close();
            onClosed();
        }
    }

    @Override
    public void exceptionCaught( ChannelHandlerContext ctx, Throwable cause )
    {
        ctx.close();
        computer.queueEvent( FAILURE_EVENT, new Object[]{
            url,
            cause instanceof WebSocketHandshakeException ? cause.getMessage() : "Could not connect"
        } );
    }

    @Nonnull
    @Override
    public String[] getMethodNames()
    {
        return new String[]{ "receive", "send", "close" };
    }

    @Nullable
    @Override
    public Object[] callMethod( @Nonnull ILuaContext context, int method, @Nonnull Object[] arguments ) throws LuaException, InterruptedException
    {
        switch( method )
        {
            case 0:
                while( true )
                {
                    checkOpen();
                    Object[] event = context.pullEvent( MESSAGE_EVENT );
                    if( event.length >= 3 && Objects.equal( event[1], url ) )
                    {
                        return new Object[]{ event[2] };
                    }
                }
            case 1:
            {
                checkOpen();
                String text = arguments.length > 0 && arguments[0] != null ? arguments[0].toString() : "";
                computer.addTrackingChange( TrackingField.WEBSOCKET_OUTGOING, text.length() );
                channel.writeAndFlush( new TextWebSocketFrame( text ) );
                return null;
            }
            case 2:
                close( true );
                return null;
            default:
                return null;
        }
    }

    private void checkOpen() throws LuaException
    {
        if( !open ) throw new LuaException( "attempt to use a closed file" );
    }
}
