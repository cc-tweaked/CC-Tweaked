/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2017. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.core.apis.http;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.core.apis.HTTPAPI;
import dan200.computercraft.core.apis.IAPIEnvironment;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshakerFactory;
import io.netty.handler.codec.http.websocketx.WebSocketVersion;
import io.netty.handler.codec.http.websocketx.extensions.compression.WebSocketClientCompressionHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;

import javax.net.ssl.SSLException;
import javax.net.ssl.TrustManagerFactory;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.KeyStore;
import java.util.Map;
import java.util.concurrent.Future;

/**
 * Provides functionality to verify and connect to a remote websocket.
 */
public final class WebsocketConnector
{
    private static final Object lock = new Object();
    private static TrustManagerFactory trustManager;

    private WebsocketConnector()
    {
    }

    private static TrustManagerFactory getTrustManager()
    {
        if( trustManager != null ) return trustManager;
        synchronized( lock )
        {
            if( trustManager != null ) return trustManager;

            TrustManagerFactory tmf = null;
            try
            {
                tmf = TrustManagerFactory.getInstance( TrustManagerFactory.getDefaultAlgorithm() );
                tmf.init( (KeyStore) null );
            }
            catch( Exception e )
            {
                ComputerCraft.log.error( "Cannot setup trust manager", e );
            }

            return trustManager = tmf;
        }
    }

    public static URI checkURI( String address ) throws HTTPRequestException
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

        if( !ComputerCraft.http_whitelist.matches( uri.getHost() ) || ComputerCraft.http_blacklist.matches( uri.getHost() ) )
        {
            throw new HTTPRequestException( "Domain not permitted" );
        }

        return uri;
    }

    public static int getPort( URI uri ) throws HTTPRequestException
    {
        int port = uri.getPort();
        if( port >= 0 ) return port;

        String scheme = uri.getScheme();
        if( scheme.equalsIgnoreCase( "ws" ) )
        {
            return 80;
        }
        else if( scheme.equalsIgnoreCase( "wss" ) )
        {
            return 443;
        }
        else
        {
            throw new HTTPRequestException( "Invalid scheme '" + scheme + "'" );
        }
    }

    public static Future<?> createConnector( final IAPIEnvironment environment, final HTTPAPI api, final URI uri, final String address, final int port, final Map<String, String> headers )
    {
        return HTTPExecutor.EXECUTOR.submit( () -> {
            InetAddress resolved;
            try
            {
                resolved = HTTPRequest.checkHost( uri.getHost() );
            }
            catch( HTTPRequestException e )
            {
                environment.queueEvent( WebsocketConnection.FAILURE_EVENT, new Object[] { address, e.getMessage() } );
                return;
            }

            InetSocketAddress socketAddress = new InetSocketAddress( resolved, uri.getPort() == -1 ? port : uri.getPort() );

            final SslContext ssl;
            if( uri.getScheme().equalsIgnoreCase( "wss" ) )
            {
                try
                {
                    ssl = SslContextBuilder.forClient().trustManager( getTrustManager() ).build();
                }
                catch( SSLException e )
                {
                    environment.queueEvent( WebsocketConnection.FAILURE_EVENT, new Object[] { address, "Cannot create secure socket" } );
                    return;
                }
            }
            else
            {
                ssl = null;
            }

            HttpHeaders httpHeaders = new DefaultHttpHeaders();
            for( Map.Entry<String, String> header : headers.entrySet() )
            {
                httpHeaders.add( header.getKey(), header.getValue() );
            }

            WebSocketClientHandshaker handshaker = WebSocketClientHandshakerFactory.newHandshaker( uri, WebSocketVersion.V13, null, true, httpHeaders );
            final WebsocketConnection connection = new WebsocketConnection( environment, api, handshaker, address );

            new Bootstrap()
                .group( HTTPExecutor.LOOP_GROUP )
                .channel( NioSocketChannel.class )
                .handler( new ChannelInitializer<SocketChannel>()
                {
                    @Override
                    protected void initChannel( SocketChannel ch ) throws Exception
                    {
                        ChannelPipeline p = ch.pipeline();
                        if( ssl != null ) p.addLast( ssl.newHandler( ch.alloc(), uri.getHost(), port ) );
                        p.addLast(
                            new HttpClientCodec(),
                            new HttpObjectAggregator( 8192 ),
                            WebSocketClientCompressionHandler.INSTANCE,
                            connection
                        );
                    }
                } )
                .remoteAddress( socketAddress )
                .connect();
        } );
    }
}
