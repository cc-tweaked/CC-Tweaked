/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.core.apis.http.websocket;

import com.google.common.base.Strings;
import dan200.computercraft.ComputerCraft;
import dan200.computercraft.core.apis.HTTPAPI;
import dan200.computercraft.core.apis.IAPIEnvironment;
import dan200.computercraft.core.apis.http.HTTPRequestException;
import dan200.computercraft.core.apis.http.MonitorerdResource;
import dan200.computercraft.core.apis.http.NetworkUtils;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshakerFactory;
import io.netty.handler.codec.http.websocketx.WebSocketVersion;
import io.netty.handler.codec.http.websocketx.extensions.compression.WebSocketClientCompressionHandler;
import io.netty.handler.ssl.SslContext;

import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.Future;

/**
 * Provides functionality to verify and connect to a remote websocket.
 */
public class Websocket extends MonitorerdResource
{
    static final String SUCCESS_EVENT = "websocket_success";
    static final String FAILURE_EVENT = "websocket_failure";
    static final String CLOSE_EVENT = "websocket_closed";
    static final String MESSAGE_EVENT = "websocket_message";

    private Future<?> executorFuture;
    private ChannelFuture connectFuture;
    private WebsocketHandler websocketHandler;

    private final IAPIEnvironment environment;
    private final HTTPAPI api;
    private final URI uri;
    private final String address;
    private final HttpHeaders headers;

    public Websocket( IAPIEnvironment environment, HTTPAPI api, URI uri, String address, HttpHeaders headers )
    {
        this.environment = environment;
        this.api = api;
        this.uri = uri;
        this.address = address;
        this.headers = headers;
    }

    public static URI checkUri( String address ) throws HTTPRequestException
    {
        URI uri = null;
        try
        {
            uri = new URI( address );
        }
        catch( URISyntaxException ignored )
        {
        }

        if( uri == null || uri.getHost() == null )
        {
            try
            {
                uri = new URI( "ws://" + address );
            }
            catch( URISyntaxException ignored )
            {
            }
        }

        if( uri == null || uri.getHost() == null ) throw new HTTPRequestException( "URL malformed" );

        String scheme = uri.getScheme();
        if( scheme == null )
        {
            try
            {
                uri = new URI( "ws://" + uri.toString() );
            }
            catch( URISyntaxException e )
            {
                throw new HTTPRequestException( "URL malformed" );
            }
        }
        else if( !scheme.equalsIgnoreCase( "wss" ) && !scheme.equalsIgnoreCase( "ws" ) )
        {
            throw new HTTPRequestException( "Invalid scheme '" + scheme + "'" );
        }

        NetworkUtils.checkHost( uri.getHost() );
        return uri;
    }

    public void connect()
    {
        if( isClosed() ) return;
        executorFuture = NetworkUtils.EXECUTOR.submit( this::doConnect );
    }

    private void doConnect()
    {
        // If we're cancelled, abort.
        if( isClosed() ) return;

        try
        {
            boolean ssl = uri.getScheme().equalsIgnoreCase( "wss" );

            InetSocketAddress socketAddress = NetworkUtils.getAddress( uri.getHost(), uri.getPort(), ssl );
            SslContext sslContext = ssl ? NetworkUtils.getSslContext() : null;

            // getAddress may have a slight delay, so let's perform another cancellation check.
            if( isClosed() ) return;

            WebSocketClientHandshaker handshaker = WebSocketClientHandshakerFactory.newHandshaker( uri, WebSocketVersion.V13, null, true, headers );
            WebsocketHandler handler = websocketHandler = new WebsocketHandler( this, handshaker, address );

            connectFuture = new Bootstrap()
                .group( NetworkUtils.LOOP_GROUP )
                .channel( NioSocketChannel.class )
                .handler( new ChannelInitializer<SocketChannel>()
                {
                    @Override
                    protected void initChannel( SocketChannel ch )
                    {
                        ChannelPipeline p = ch.pipeline();
                        if( sslContext != null )
                        {
                            p.addLast( sslContext.newHandler( ch.alloc(), uri.getHost(), socketAddress.getPort() ) );
                        }
                        p.addLast(
                            new HttpClientCodec(),
                            new HttpObjectAggregator( 8192 ),
                            WebSocketClientCompressionHandler.INSTANCE,
                            handler
                        );
                    }
                } )
                .remoteAddress( socketAddress )
                .connect();

            // Do an additional check for cancellation
            checkClosed();
        }
        catch( HTTPRequestException e )
        {
            failure( e.getMessage() );
        }
        catch( Exception e )
        {
            failure( "Could not connect" );
            if( ComputerCraft.logPeripheralErrors ) ComputerCraft.log.error( "Error in HTTP request", e );
        }
    }

    void failure( String message )
    {
        if( tryClose() ) environment.queueEvent( FAILURE_EVENT, new Object[] { address, message } );
    }

    void close( int status, String reason )
    {
        if( tryClose() )
        {
            environment.queueEvent( CLOSE_EVENT, new Object[] {
                address,
                Strings.isNullOrEmpty( reason ) ? null : reason,
                status < 0 ? null : status,
            } );
        }
    }

    @Override
    protected void dispose()
    {
        api.removeCloseable( this );

        executorFuture = closeFuture( executorFuture );
        connectFuture = closeChannel( connectFuture );
        websocketHandler = closeCloseable( websocketHandler );
    }

    public IAPIEnvironment environment()
    {
        return environment;
    }
}
