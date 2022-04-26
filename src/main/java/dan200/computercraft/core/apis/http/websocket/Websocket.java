/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.core.apis.http.websocket;

import com.google.common.base.Strings;
import dan200.computercraft.ComputerCraft;
import dan200.computercraft.core.apis.IAPIEnvironment;
import dan200.computercraft.core.apis.http.HTTPRequestException;
import dan200.computercraft.core.apis.http.NetworkUtils;
import dan200.computercraft.core.apis.http.Resource;
import dan200.computercraft.core.apis.http.ResourceGroup;
import dan200.computercraft.core.apis.http.options.Options;
import dan200.computercraft.shared.util.IoUtil;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshakerFactory;
import io.netty.handler.codec.http.websocketx.WebSocketVersion;
import io.netty.handler.ssl.SslContext;

import java.lang.ref.WeakReference;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.Future;

/**
 * Provides functionality to verify and connect to a remote websocket.
 */
public class Websocket extends Resource<Websocket>
{
    /**
     * We declare the maximum size to be 2^30 bytes. While messages can be much longer, we set an arbitrary limit as
     * working with larger messages (especially within a Lua VM) is absurd.
     */
    public static final int MAX_MESSAGE_SIZE = 1 << 30;

    static final String SUCCESS_EVENT = "websocket_success";
    static final String FAILURE_EVENT = "websocket_failure";
    static final String CLOSE_EVENT = "websocket_closed";
    static final String MESSAGE_EVENT = "websocket_message";

    private Future<?> executorFuture;
    private ChannelFuture connectFuture;
    private WeakReference<WebsocketHandle> websocketHandle;

    private final IAPIEnvironment environment;
    private final URI uri;
    private final String address;
    private final HttpHeaders headers;

    public Websocket( ResourceGroup<Websocket> limiter, IAPIEnvironment environment, URI uri, String address, HttpHeaders headers )
    {
        super( limiter );
        this.environment = environment;
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
                uri = new URI( "ws://" + uri );
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

        return uri;
    }

    public void connect()
    {
        if( isClosed() ) return;
        executorFuture = NetworkUtils.EXECUTOR.submit( this::doConnect );
        checkClosed();
    }

    private void doConnect()
    {
        // If we're cancelled, abort.
        if( isClosed() ) return;

        try
        {
            boolean ssl = uri.getScheme().equalsIgnoreCase( "wss" );
            InetSocketAddress socketAddress = NetworkUtils.getAddress( uri, ssl );
            Options options = NetworkUtils.getOptions( uri.getHost(), socketAddress );
            SslContext sslContext = ssl ? NetworkUtils.getSslContext() : null;

            // getAddress may have a slight delay, so let's perform another cancellation check.
            if( isClosed() ) return;

            connectFuture = new Bootstrap()
                .group( NetworkUtils.LOOP_GROUP )
                .channel( NioSocketChannel.class )
                .handler( new ChannelInitializer<SocketChannel>()
                {
                    @Override
                    protected void initChannel( SocketChannel ch )
                    {
                        ChannelPipeline p = ch.pipeline();
                        p.addLast( NetworkUtils.SHAPING_HANDLER );
                        if( sslContext != null )
                        {
                            p.addLast( sslContext.newHandler( ch.alloc(), uri.getHost(), socketAddress.getPort() ) );
                        }

                        String subprotocol = headers.get( HttpHeaderNames.SEC_WEBSOCKET_PROTOCOL );
                        WebSocketClientHandshaker handshaker = WebSocketClientHandshakerFactory.newHandshaker(
                            uri, WebSocketVersion.V13, subprotocol, true, headers,
                            options.websocketMessage <= 0 ? MAX_MESSAGE_SIZE : options.websocketMessage
                        );

                        p.addLast(
                            new HttpClientCodec(),
                            new HttpObjectAggregator( 8192 ),
                            WebsocketCompressionHandler.INSTANCE,
                            new WebsocketHandler( Websocket.this, handshaker, options )
                        );
                    }
                } )
                .remoteAddress( socketAddress )
                .connect()
                .addListener( c -> {
                    if( !c.isSuccess() ) failure( NetworkUtils.toFriendlyError( c.cause() ) );
                } );

            // Do an additional check for cancellation
            checkClosed();
        }
        catch( HTTPRequestException e )
        {
            failure( e.getMessage() );
        }
        catch( Exception e )
        {
            failure( NetworkUtils.toFriendlyError( e ) );
            if( ComputerCraft.logComputerErrors ) ComputerCraft.log.error( "Error in websocket", e );
        }
    }

    void success( Channel channel, Options options )
    {
        if( isClosed() ) return;

        WebsocketHandle handle = new WebsocketHandle( this, options, channel );
        environment().queueEvent( SUCCESS_EVENT, address, handle );
        websocketHandle = createOwnerReference( handle );

        checkClosed();
    }

    void failure( String message )
    {
        if( tryClose() ) environment.queueEvent( FAILURE_EVENT, address, message );
    }

    void close( int status, String reason )
    {
        if( tryClose() )
        {
            environment.queueEvent( CLOSE_EVENT, address,
                Strings.isNullOrEmpty( reason ) ? null : reason,
                status < 0 ? null : status );
        }
    }

    @Override
    protected void dispose()
    {
        super.dispose();

        executorFuture = closeFuture( executorFuture );
        connectFuture = closeChannel( connectFuture );

        WeakReference<WebsocketHandle> websocketHandleRef = websocketHandle;
        WebsocketHandle websocketHandle = websocketHandleRef == null ? null : websocketHandleRef.get();
        IoUtil.closeQuietly( websocketHandle );
        this.websocketHandle = null;
    }

    public IAPIEnvironment environment()
    {
        return environment;
    }

    public String address()
    {
        return address;
    }
}
