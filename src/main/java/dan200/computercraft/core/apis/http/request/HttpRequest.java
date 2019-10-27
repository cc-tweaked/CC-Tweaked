/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.core.apis.http.request;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.api.lua.ILuaObject;
import dan200.computercraft.core.apis.IAPIEnvironment;
import dan200.computercraft.core.apis.http.HTTPRequestException;
import dan200.computercraft.core.apis.http.NetworkUtils;
import dan200.computercraft.core.apis.http.Resource;
import dan200.computercraft.core.apis.http.ResourceGroup;
import dan200.computercraft.core.tracking.TrackingField;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.ConnectTimeoutException;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.TooLongFrameException;
import io.netty.handler.codec.http.*;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.timeout.ReadTimeoutException;
import io.netty.handler.timeout.ReadTimeoutHandler;

import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Represents an in-progress HTTP request.
 */
public class HttpRequest extends Resource<HttpRequest>
{
    private static final String SUCCESS_EVENT = "http_success";
    private static final String FAILURE_EVENT = "http_failure";

    private static final int MAX_REDIRECTS = 16;

    private Future<?> executorFuture;
    private ChannelFuture connectFuture;
    private HttpRequestHandler currentRequest;

    private final IAPIEnvironment environment;

    private final String address;
    private final ByteBuf postBuffer;
    private final HttpHeaders headers;
    private final boolean binary;

    final AtomicInteger redirects;

    public HttpRequest( ResourceGroup<HttpRequest> limiter, IAPIEnvironment environment, String address, String postText, HttpHeaders headers, boolean binary, boolean followRedirects )
    {
        super( limiter );
        this.environment = environment;
        this.address = address;
        postBuffer = postText != null
            ? Unpooled.wrappedBuffer( postText.getBytes( StandardCharsets.UTF_8 ) )
            : Unpooled.buffer( 0 );
        this.headers = headers;
        this.binary = binary;
        redirects = new AtomicInteger( followRedirects ? MAX_REDIRECTS : 0 );

        if( postText != null )
        {
            if( !headers.contains( HttpHeaderNames.CONTENT_TYPE ) )
            {
                headers.set( HttpHeaderNames.CONTENT_TYPE, "application/x-www-form-urlencoded; charset=utf-8" );
            }

            if( !headers.contains( HttpHeaderNames.CONTENT_LENGTH ) )
            {
                headers.set( HttpHeaderNames.CONTENT_LENGTH, postBuffer.readableBytes() );
            }
        }
    }

    public IAPIEnvironment environment()
    {
        return environment;
    }

    public static URI checkUri( String address ) throws HTTPRequestException
    {
        URI url;
        try
        {
            url = new URI( address );
        }
        catch( URISyntaxException e )
        {
            throw new HTTPRequestException( "URL malformed" );
        }

        checkUri( url );
        return url;
    }

    public static void checkUri( URI url ) throws HTTPRequestException
    {
        // Validate the URL
        if( url.getScheme() == null ) throw new HTTPRequestException( "Must specify http or https" );
        if( url.getHost() == null ) throw new HTTPRequestException( "URL malformed" );

        String scheme = url.getScheme().toLowerCase( Locale.ROOT );
        if( !scheme.equalsIgnoreCase( "http" ) && !scheme.equalsIgnoreCase( "https" ) )
        {
            throw new HTTPRequestException( "Invalid protocol '" + scheme + "'" );
        }

        NetworkUtils.checkHost( url.getHost() );
    }

    public void request( URI uri, HttpMethod method )
    {
        if( isClosed() ) return;
        executorFuture = NetworkUtils.EXECUTOR.submit( () -> doRequest( uri, method ) );
        checkClosed();
    }

    private void doRequest( URI uri, HttpMethod method )
    {
        // If we're cancelled, abort.
        if( isClosed() ) return;

        try
        {
            boolean ssl = uri.getScheme().equalsIgnoreCase( "https" );
            InetSocketAddress socketAddress = NetworkUtils.getAddress( uri.getHost(), uri.getPort(), ssl );
            SslContext sslContext = ssl ? NetworkUtils.getSslContext() : null;

            // getAddress may have a slight delay, so let's perform another cancellation check.
            if( isClosed() ) return;

            // Add request size to the tracker before opening the connection
            environment.addTrackingChange( TrackingField.HTTP_REQUESTS, 1 );
            environment.addTrackingChange( TrackingField.HTTP_UPLOAD, getHeaderSize( headers ) + postBuffer.capacity() );

            HttpRequestHandler handler = currentRequest = new HttpRequestHandler( this, uri, method );
            connectFuture = new Bootstrap()
                .group( NetworkUtils.LOOP_GROUP )
                .channelFactory( NioSocketChannel::new )
                .handler( new ChannelInitializer<SocketChannel>()
                {
                    @Override
                    protected void initChannel( SocketChannel ch )
                    {

                        if( ComputerCraft.httpTimeout > 0 )
                        {
                            ch.config().setConnectTimeoutMillis( ComputerCraft.httpTimeout );
                        }

                        ChannelPipeline p = ch.pipeline();
                        if( sslContext != null )
                        {
                            p.addLast( sslContext.newHandler( ch.alloc(), uri.getHost(), socketAddress.getPort() ) );
                        }

                        if( ComputerCraft.httpTimeout > 0 )
                        {
                            p.addLast( new ReadTimeoutHandler( ComputerCraft.httpTimeout, TimeUnit.MILLISECONDS ) );
                        }

                        p.addLast(
                            new HttpClientCodec(),
                            new HttpContentDecompressor(),
                            handler
                        );
                    }
                } )
                .remoteAddress( socketAddress )
                .connect()
                .addListener( c -> {
                    if( !c.isSuccess() ) failure( c.cause() );
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
            failure( "Could not connect" );
            if( ComputerCraft.logPeripheralErrors ) ComputerCraft.log.error( "Error in HTTP request", e );
        }
    }

    void failure( String message )
    {
        if( tryClose() ) environment.queueEvent( FAILURE_EVENT, new Object[] { address, message } );
    }

    void failure( Throwable cause )
    {
        String message;
        if( cause instanceof HTTPRequestException )
        {
            message = cause.getMessage();
        }
        else if( cause instanceof TooLongFrameException )
        {
            message = "Response is too large";
        }
        else if( cause instanceof ReadTimeoutException || cause instanceof ConnectTimeoutException )
        {
            message = "Timed out";
        }
        else
        {
            message = "Could not connect";
        }

        failure( message );
    }

    void failure( String message, ILuaObject object )
    {
        if( tryClose() ) environment.queueEvent( FAILURE_EVENT, new Object[] { address, message, object } );
    }

    void success( ILuaObject object )
    {
        if( tryClose() ) environment.queueEvent( SUCCESS_EVENT, new Object[] { address, object } );
    }

    @Override
    protected void dispose()
    {
        super.dispose();

        executorFuture = closeFuture( executorFuture );
        connectFuture = closeChannel( connectFuture );
        currentRequest = closeCloseable( currentRequest );
    }

    public static long getHeaderSize( HttpHeaders headers )
    {
        long size = 0;
        for( Map.Entry<String, String> header : headers )
        {
            size += header.getKey() == null ? 0 : header.getKey().length();
            size += header.getValue() == null ? 0 : header.getValue().length() + 1;
        }
        return size;
    }

    public ByteBuf body()
    {
        return postBuffer;
    }

    public HttpHeaders headers()
    {
        return headers;
    }

    public boolean isBinary()
    {
        return binary;
    }
}
