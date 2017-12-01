/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2017. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.core.apis;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.api.lua.ILuaAPI;
import dan200.computercraft.api.lua.ILuaContext;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.core.apis.http.*;

import javax.annotation.Nonnull;
import java.io.Closeable;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.*;
import java.util.concurrent.Future;

import static dan200.computercraft.core.apis.ArgumentHelper.*;

public class HTTPAPI implements ILuaAPI
{
    private final IAPIEnvironment m_apiEnvironment;
    private final List<Future<?>> m_httpTasks;
    private final Set<Closeable> m_closeables;

    public HTTPAPI( IAPIEnvironment environment )
    {
        m_apiEnvironment = environment;
        m_httpTasks = new ArrayList<>();
        m_closeables = new HashSet<>();
    }

    @Override
    public String[] getNames()
    {
        return new String[] {
            "http"
        };
    }

    @Override
    public void update()
    {
        // Wait for all of our http requests
        synchronized( m_httpTasks )
        {
            Iterator<Future<?>> it = m_httpTasks.iterator();
            while( it.hasNext() )
            {
                final Future<?> h = it.next();
                if( h.isDone() ) it.remove();
            }
        }
    }

    @Override
    public void shutdown( )
    {
        synchronized( m_httpTasks )
        {
            for( Future<?> r : m_httpTasks )
            {
                r.cancel( false );
            }
            m_httpTasks.clear();
        }
        synchronized( m_closeables )
        {
            for( Closeable x : m_closeables )
            {
                try
                {
                    x.close();
                }
                catch( IOException ignored )
                {
                }
            }
            m_closeables.clear();
        }
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
    public Object[] callMethod( @Nonnull ILuaContext context, int method, @Nonnull Object[] args ) throws LuaException
    {
        switch( method )
        {
            case 0:
            {
                // request
                // Get URL
                String urlString = getString( args, 0 );

                // Get POST
                String postString = optString( args, 1, null );

                // Get Headers
                Map<String, String> headers = null;
                Map<Object, Object> table = optTable( args, 2, null );
                if( table != null )
                {
                    headers = new HashMap<>( table.size() );
                    for( Object key : table.keySet() )
                    {
                        Object value = table.get( key );
                        if( key instanceof String && value instanceof String )
                        {
                            headers.put( (String)key, (String)value );
                        }
                    }
                }

                // Get binary
                boolean binary = false;
                if( args.length >= 4 )
                {
                    binary = args[ 3 ] != null && !args[ 3 ].equals( Boolean.FALSE );
                }

                // Make the request
                try
                {
                    URL url = HTTPRequest.checkURL( urlString );
                    HTTPRequest request = new HTTPRequest( m_apiEnvironment, urlString, url, postString, headers, binary );
                    synchronized( m_httpTasks )
                    {
                        m_httpTasks.add( HTTPExecutor.EXECUTOR.submit( request ) );
                    }
                    return new Object[] { true };
                }
                catch( HTTPRequestException e )
                {
                    return new Object[] { false, e.getMessage() };
                }
            }
            case 1:
            {
                // checkURL
                // Get URL
                String urlString = getString( args, 0 );

                // Check URL
                try
                {
                    URL url = HTTPRequest.checkURL( urlString );
                    HTTPCheck check = new HTTPCheck( m_apiEnvironment, urlString, url );
                    synchronized( m_httpTasks )
                    {
                        m_httpTasks.add( HTTPExecutor.EXECUTOR.submit( check ) );
                    }
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
                Map<Object, Object> headerTbl = optTable( args, 1, Collections.emptyMap() );

                HashMap<String, String> headers = new HashMap<String, String>( headerTbl.size() );
                for( Object key : headerTbl.keySet() )
                {
                    Object value = headerTbl.get( key );
                    if( key instanceof String && value instanceof String )
                    {
                        headers.put( (String) key, (String) value );
                    }
                }

                if( !ComputerCraft.http_websocket_enable )
                {
                    throw new LuaException( "Websocket connections are disabled" );
                }

                try
                {
                    URI uri = WebsocketConnector.checkURI( address );
                    int port = WebsocketConnector.getPort( uri );

                    Future<?> connector = WebsocketConnector.createConnector( m_apiEnvironment, this, uri, address, port, headers );
                    synchronized( m_httpTasks )
                    {
                        m_httpTasks.add( connector );
                    }
                    return new Object[] { true };
                }
                catch( HTTPRequestException e )
                {
                    return new Object[] { false, e.getMessage() };
                }
            }
            default:
            {
                return null;
            }
        }
    }

    public void addCloseable( Closeable closeable )
    {
        synchronized( m_closeables )
        {
            m_closeables.add( closeable );
        }
    }

    public void removeCloseable( Closeable closeable )
    {
        synchronized( m_closeables )
        {
            m_closeables.remove( closeable );
        }
    }
}
