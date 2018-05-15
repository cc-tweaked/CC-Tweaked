/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2017. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.core.apis.http;

import com.google.common.base.Joiner;
import com.google.common.io.ByteStreams;
import dan200.computercraft.ComputerCraft;
import dan200.computercraft.api.lua.ILuaContext;
import dan200.computercraft.api.lua.ILuaObject;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.core.apis.IAPIEnvironment;
import dan200.computercraft.core.apis.handles.BinaryInputHandle;
import dan200.computercraft.core.apis.handles.EncodedInputHandle;
import dan200.computercraft.core.tracking.TrackingField;

import javax.annotation.Nonnull;
import java.io.*;
import java.net.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HTTPRequest implements Runnable
{
    public static URL checkURL( String urlString ) throws HTTPRequestException
    {
        URL url;
        try
        {
            url = new URL( urlString );
        }
        catch( MalformedURLException e )
        {
            throw new HTTPRequestException( "URL malformed" );
        }

        // Validate the URL
        String protocol = url.getProtocol().toLowerCase();
        if( !protocol.equals( "http" ) && !protocol.equals( "https" ) )
        {
            throw new HTTPRequestException( "URL not http" );
        }

        // Compare the URL to the whitelist
        if( !ComputerCraft.http_whitelist.matches( url.getHost() ) || ComputerCraft.http_blacklist.matches( url.getHost() ) )
        {
            throw new HTTPRequestException( "Domain not permitted" );
        }

        return url;
    }

    public static InetAddress checkHost( String host ) throws HTTPRequestException
    {
        try
        {
            InetAddress resolved = InetAddress.getByName( host );
            if( !ComputerCraft.http_whitelist.matches( resolved ) || ComputerCraft.http_blacklist.matches( resolved ) )
            {
                throw new HTTPRequestException( "Domain not permitted" );
            }

            return resolved;
        }
        catch( UnknownHostException e )
        {
            throw new HTTPRequestException( "Unknown host" );
        }
    }

    private final IAPIEnvironment m_environment;
    private final URL m_url;
    private final String m_urlString;
    private final String m_postText;
    private final Map<String, String> m_headers;
    private boolean m_binary;
    private final String m_method;
    private final boolean m_followRedirects;

    public HTTPRequest( IAPIEnvironment environment, String urlString, URL url, final String postText, final Map<String, String> headers, boolean binary, final String method, final boolean followRedirects ) throws HTTPRequestException
    {
        m_environment = environment;
        m_urlString = urlString;
        m_url = url;
        m_binary = binary;
        m_postText = postText;
        m_headers = headers;
        m_method = method;
        m_followRedirects = followRedirects;
    }

    @Override
    public void run()
    {
        // First verify the address is allowed.
        try
        {
            checkHost( m_url.getHost() );
        }
        catch( HTTPRequestException e )
        {
            // Queue the failure event if not.
            String error = e.getMessage();
            m_environment.queueEvent( "http_failure", new Object[]{ m_urlString, error == null ? "Could not connect" : error, null } );
            return;
        }

        try
        {
            // Connect to the URL
            HttpURLConnection connection = (HttpURLConnection) m_url.openConnection();

            if( m_postText != null )
            {
                connection.setRequestMethod( "POST" );
                connection.setDoOutput( true );
            }
            else
            {
                connection.setRequestMethod( "GET" );
            }
            if( m_method != null ) connection.setRequestMethod( m_method );
            connection.setInstanceFollowRedirects( m_followRedirects );

            // Set headers
            connection.setRequestProperty( "accept-charset", "UTF-8" );
            if( m_postText != null )
            {
                connection.setRequestProperty( "content-type", "application/x-www-form-urlencoded; charset=utf-8" );
            }
            if( m_headers != null )
            {
                for( Map.Entry<String, String> header : m_headers.entrySet() )
                {
                    connection.setRequestProperty( header.getKey(), header.getValue() );
                }
            }

            // Add request size and count to the tracker before opening the connection
            m_environment.addTrackingChange( TrackingField.HTTP_REQUESTS );
            m_environment.addTrackingChange( TrackingField.HTTP_UPLOAD,
                getHeaderSize( connection.getRequestProperties() ) + (m_postText == null ? 0 : m_postText.length()) );

            // Send POST text
            if( m_postText != null )
            {
                OutputStream os = connection.getOutputStream();
                OutputStreamWriter osw;
                try
                {
                    osw = new OutputStreamWriter( os, "UTF-8" );
                }
                catch( UnsupportedEncodingException e )
                {
                    osw = new OutputStreamWriter( os );
                }
                BufferedWriter writer = new BufferedWriter( osw );
                writer.write( m_postText, 0, m_postText.length() );
                writer.close();
            }

            // Read response
            InputStream is;
            int code = connection.getResponseCode();
            boolean responseSuccess;
            if( code >= 200 && code < 400 )
            {
                is = connection.getInputStream();
                responseSuccess = true;
            }
            else
            {
                is = connection.getErrorStream();
                responseSuccess = false;
            }

            byte[] result = ByteStreams.toByteArray( is );
            is.close();

            // We've got some sort of response, so let's build a resulting object.
            Joiner joiner = Joiner.on( ',' );
            Map<String, String> headers = new HashMap<>();
            for( Map.Entry<String, List<String>> header : connection.getHeaderFields().entrySet() )
            {
                headers.put( header.getKey(), joiner.join( header.getValue() ) );
            }

            m_environment.addTrackingChange( TrackingField.HTTP_DOWNLOAD,
                getHeaderSize( connection.getHeaderFields() ) + result.length );

            InputStream contents = new ByteArrayInputStream( result );
            ILuaObject stream = wrapStream(
                m_binary ? new BinaryInputHandle( contents ) : new EncodedInputHandle( contents, connection.getContentEncoding() ),
                connection.getResponseCode(), headers
            );

            connection.disconnect(); // disconnect

            // Queue the appropriate event.
            if( responseSuccess )
            {
                m_environment.queueEvent( "http_success", new Object[]{ m_urlString, stream } );
            }
            else
            {
                m_environment.queueEvent( "http_failure", new Object[]{ m_urlString, "Could not connect", stream } );
            }
        }
        catch( IOException e )
        {
            // There was an error
            m_environment.queueEvent( "http_failure", new Object[]{ m_urlString, "Could not connect", null } );
        }
    }

    private static ILuaObject wrapStream( final ILuaObject reader, final int responseCode, final Map<String, String> responseHeaders )
    {
        String[] oldMethods = reader.getMethodNames();
        final int methodOffset = oldMethods.length;

        final String[] newMethods = Arrays.copyOf( oldMethods, oldMethods.length + 2 );
        newMethods[methodOffset + 0] = "getResponseCode";
        newMethods[methodOffset + 1] = "getResponseHeaders";

        return new ILuaObject()
        {
            @Nonnull
            @Override
            public String[] getMethodNames()
            {
                return newMethods;
            }

            @Override
            public Object[] callMethod( @Nonnull ILuaContext context, int method, @Nonnull Object[] args ) throws LuaException, InterruptedException
            {
                if( method < methodOffset )
                {
                    return reader.callMethod( context, method, args );
                }
                switch( method - methodOffset )
                {
                    case 0:
                    {
                        // getResponseCode
                        return new Object[]{ responseCode };
                    }
                    case 1:
                    {
                        // getResponseHeaders
                        return new Object[]{ responseHeaders };
                    }
                    default:
                    {
                        return null;
                    }
                }
            }
        };
    }

    private static long getHeaderSize( Map<String, List<String>> headers )
    {
        long size = 0;
        for( Map.Entry<String, List<String>> header : headers.entrySet() )
        {
            size += header.getKey() == null ? 0 : header.getKey().length();
            for( String value : header.getValue() ) size += value == null ? 0 : value.length() + 1;
        }
        return size;
    }
}
