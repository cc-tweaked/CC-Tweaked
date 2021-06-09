/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.core.apis.http.websocket;

import dan200.computercraft.core.apis.http.HTTPRequestException;
import dan200.computercraft.core.apis.http.NetworkUtils;
import dan200.computercraft.core.apis.http.options.Options;
import dan200.computercraft.core.tracking.TrackingField;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ConnectTimeoutException;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.TooLongFrameException;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.websocketx.*;
import io.netty.handler.timeout.ReadTimeoutException;
import io.netty.util.CharsetUtil;

import static dan200.computercraft.core.apis.http.websocket.Websocket.MESSAGE_EVENT;

public class WebsocketHandler extends SimpleChannelInboundHandler<Object>
{
    private final Websocket websocket;
    private final WebSocketClientHandshaker handshaker;
    private final Options options;

    public WebsocketHandler( Websocket websocket, WebSocketClientHandshaker handshaker, Options options )
    {
        this.handshaker = handshaker;
        this.websocket = websocket;
        this.options = options;
    }

    @Override
    public void channelActive( ChannelHandlerContext ctx ) throws Exception
    {
        this.handshaker.handshake( ctx.channel() );
        super.channelActive( ctx );
    }

    @Override
    public void channelInactive( ChannelHandlerContext ctx ) throws Exception
    {
        this.websocket.close( -1, "Websocket is inactive" );
        super.channelInactive( ctx );
    }

    @Override
    public void exceptionCaught( ChannelHandlerContext ctx, Throwable cause )
    {
        ctx.close();

        String message;
        if( cause instanceof WebSocketHandshakeException || cause instanceof HTTPRequestException )
        {
            message = cause.getMessage();
        }
        else if( cause instanceof TooLongFrameException )
        {
            message = "Message is too large";
        }
        else if( cause instanceof ReadTimeoutException || cause instanceof ConnectTimeoutException )
        {
            message = "Timed out";
        }
        else
        {
            message = "Could not connect";
        }

        if( this.handshaker.isHandshakeComplete() )
        {
            this.websocket.close( -1, message );
        }
        else
        {
            this.websocket.failure( message );
        }
    }

    @Override
    public void channelRead0( ChannelHandlerContext ctx, Object msg )
    {
        if( this.websocket.isClosed() )
        {
            return;
        }

        if( !this.handshaker.isHandshakeComplete() )
        {
            this.handshaker.finishHandshake( ctx.channel(), (FullHttpResponse) msg );
            this.websocket.success( ctx.channel(), this.options );
            return;
        }

        if( msg instanceof FullHttpResponse )
        {
            FullHttpResponse response = (FullHttpResponse) msg;
            throw new IllegalStateException( "Unexpected FullHttpResponse (getStatus=" + response.status() + ", content=" + response.content()
                .toString( CharsetUtil.UTF_8 ) + ')' );
        }

        WebSocketFrame frame = (WebSocketFrame) msg;
        if( frame instanceof TextWebSocketFrame )
        {
            String data = ((TextWebSocketFrame) frame).text();

            this.websocket.environment()
                .addTrackingChange( TrackingField.WEBSOCKET_INCOMING, data.length() );
            this.websocket.environment()
                .queueEvent( MESSAGE_EVENT, this.websocket.address(), data, false );
        }
        else if( frame instanceof BinaryWebSocketFrame )
        {
            byte[] converted = NetworkUtils.toBytes( frame.content() );

            this.websocket.environment()
                .addTrackingChange( TrackingField.WEBSOCKET_INCOMING, converted.length );
            this.websocket.environment()
                .queueEvent( MESSAGE_EVENT, this.websocket.address(), converted, true );
        }
        else if( frame instanceof CloseWebSocketFrame )
        {
            CloseWebSocketFrame closeFrame = (CloseWebSocketFrame) frame;
            this.websocket.close( closeFrame.statusCode(), closeFrame.reasonText() );
        }
        else if( frame instanceof PingWebSocketFrame )
        {
            frame.content()
                .retain();
            ctx.channel()
                .writeAndFlush( new PongWebSocketFrame( frame.content() ) );
        }
    }
}
