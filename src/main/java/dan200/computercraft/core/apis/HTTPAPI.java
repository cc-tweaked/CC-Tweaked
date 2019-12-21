/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.core.apis;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.api.lua.ILuaAPI;
import dan200.computercraft.api.lua.ILuaContext;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.core.apis.http.*;
import dan200.computercraft.core.apis.http.request.HttpRequest;
import dan200.computercraft.core.apis.http.websocket.Websocket;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;

import javax.annotation.Nonnull;
import java.net.URI;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;

import static dan200.computercraft.api.lua.ArgumentHelper.*;
import static dan200.computercraft.core.apis.TableHelper.*;

public class HTTPAPI implements ILuaAPI
{
    private final IAPIEnvironment m_apiEnvironment;

    private final ResourceGroup<CheckUrl> checkUrls = new ResourceGroup<>();
    private final ResourceGroup<HttpRequest> requests = new ResourceQueue<>( () -> ComputerCraft.httpMaxRequests );
    private final ResourceGroup<Websocket> websockets = new ResourceGroup<>( () -> ComputerCraft.httpMaxWebsockets );

    public HTTPAPI( IAPIEnvironment environment )
    {
        m_apiEnvironment = environment;
    }

    @Override
    public String[] getNames()
    {
        return new String[] { "http" };
    }

    @Override
    public void startup()
    {
        checkUrls.startup();
        requests.startup();
        websockets.startup();
    }

    @Override
    public void shutdown()
    {
        checkUrls.shutdown();
        requests.shutdown();
        websockets.shutdown();
    }

    @Override
    public void update()
    {
        // It's rather ugly to run this here, but we need to clean up
        // resources as often as possible to reduce blocking.
        Resource.cleanup();
    }

    @Nonnull
    @Override
    public String[] getMethodNames()
    {
        return new String[] {
            "request",
            "checkURL",
            "websocket",
        };
    }

    @Override
    @SuppressWarnings( "resource" )
    public Object[] callMethod( @Nonnull ILuaContext context, int method, @Nonnull Object[] args ) throws LuaException
    {
        switch( method )
        {
            case 0: // request
            {
                String address, postString, requestMethod;
                Map<?, ?> headerTable;
                boolean binary, redirect;

                if( args.length >= 1 && args[0] instanceof Map )
                {
                    Map<?, ?> options = (Map<?, ?>) args[0];
                    address = getStringField( options, "url" );
                    postString = optStringField( options, "body", null );
                    headerTable = optTableField( options, "headers", Collections.emptyMap() );
                    binary = optBooleanField( options, "binary", false );
                    requestMethod = optStringField( options, "method", null );
                    redirect = optBooleanField( options, "redirect", true );

                }
                else
                {
                    // Get URL and post information
                    address = getString( args, 0 );
                    postString = optString( args, 1, null );
                    headerTable = optTable( args, 2, Collections.emptyMap() );
                    binary = optBoolean( args, 3, false );
                    requestMethod = null;
                    redirect = true;
                }

                HttpHeaders headers = getHeaders( headerTable );


                HttpMethod httpMethod;
                if( requestMethod == null )
                {
                    httpMethod = postString == null ? HttpMethod.GET : HttpMethod.POST;
                }
                else
                {
                    httpMethod = HttpMethod.valueOf( requestMethod.toUpperCase( Locale.ROOT ) );
                    if( httpMethod == null || requestMethod.equalsIgnoreCase( "CONNECT" ) )
                    {
                        throw new LuaException( "Unsupported HTTP method" );
                    }
                }

                try
                {
                    URI uri = HttpRequest.checkUri( address );
                    HttpRequest request = new HttpRequest( requests, m_apiEnvironment, address, postString, headers, binary, redirect );

                    long requestBody = request.body().readableBytes() + HttpRequest.getHeaderSize( headers );
                    if( ComputerCraft.httpMaxUpload != 0 && requestBody > ComputerCraft.httpMaxUpload )
                    {
                        throw new HTTPRequestException( "Request body is too large" );
                    }

                    // Make the request
                    request.queue( r -> r.request( uri, httpMethod ) );

                    return new Object[] { true };
                }
                catch( HTTPRequestException e )
                {
                    return new Object[] { false, e.getMessage() };
                }
            }
            case 1: // checkURL
            {
                String address = getString( args, 0 );

                // Check URL
                try
                {
                    URI uri = HttpRequest.checkUri( address );
                    new CheckUrl( checkUrls, m_apiEnvironment, address, uri ).queue( CheckUrl::run );

                    return new Object[] { true };
                }
                catch( HTTPRequestException e )
                {
                    return new Object[] { false, e.getMessage() };
                }
            }
            case 2: // websocket
            {
                String address = getString( args, 0 );
                Map<?, ?> headerTbl = optTable( args, 1, Collections.emptyMap() );

                if( !ComputerCraft.http_websocket_enable )
                {
                    throw new LuaException( "Websocket connections are disabled" );
                }

                HttpHeaders headers = getHeaders( headerTbl );

                try
                {
                    URI uri = Websocket.checkUri( address );
                    if( !new Websocket( websockets, m_apiEnvironment, uri, address, headers ).queue( Websocket::connect ) )
                    {
                        throw new LuaException( "Too many websockets already open" );
                    }

                    return new Object[] { true };
                }
                catch( HTTPRequestException e )
                {
                    return new Object[] { false, e.getMessage() };
                }
            }
            default:
                return null;
        }
    }

    @Nonnull
    private static HttpHeaders getHeaders( @Nonnull Map<?, ?> headerTable ) throws LuaException
    {
        HttpHeaders headers = new DefaultHttpHeaders();
        for( Map.Entry<?, ?> entry : headerTable.entrySet() )
        {
            Object value = entry.getValue();
            if( entry.getKey() instanceof String && value instanceof String )
            {
                try
                {
                    headers.add( (String) entry.getKey(), value );
                }
                catch( IllegalArgumentException e )
                {
                    throw new LuaException( e.getMessage() );
                }
            }
        }
        return headers;
    }
}
