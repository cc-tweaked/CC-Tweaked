package dan200.computercraft.core.apis.handles;

import dan200.computercraft.api.lua.ILuaContext;
import dan200.computercraft.api.lua.LuaException;

import javax.annotation.Nonnull;
import java.io.*;

import static dan200.computercraft.core.apis.ArgumentHelper.optInt;

public class EncodedInputHandle extends HandleGeneric
{
    private static final int BUFFER_SIZE = 8192;

    private final BufferedReader m_reader;

    public EncodedInputHandle( BufferedReader reader )
    {
        super( reader );
        this.m_reader = reader;
    }

    public EncodedInputHandle( InputStream stream )
    {
        this( stream, "UTF-8" );
    }

    public EncodedInputHandle( InputStream stream, String encoding )
    {
        this( makeReader( stream, encoding ) );
    }

    private static BufferedReader makeReader( InputStream stream, String encoding )
    {
        if( encoding == null ) encoding = "UTF-8";
        InputStreamReader streamReader;
        try
        {
            streamReader = new InputStreamReader( stream, encoding );
        }
        catch( UnsupportedEncodingException e )
        {
            streamReader = new InputStreamReader( stream );
        }
        return new BufferedReader( streamReader );
    }

    @Nonnull
    @Override
    public String[] getMethodNames()
    {
        return new String[] {
            "readLine",
            "readAll",
            "close",
            "read",
        };
    }

    @Override
    public Object[] callMethod( @Nonnull ILuaContext context, int method, @Nonnull Object[] args ) throws LuaException
    {
        switch( method )
        {
            case 0:
                // readLine
                checkOpen();
                try
                {
                    String line = m_reader.readLine();
                    if( line != null )
                    {
                        return new Object[] { line };
                    }
                    else
                    {
                        return null;
                    }
                }
                catch( IOException e )
                {
                    return null;
                }
            case 1:
                // readAll
                checkOpen();
                try
                {
                    StringBuilder result = new StringBuilder( "" );
                    String line = m_reader.readLine();
                    while( line != null )
                    {
                        result.append( line );
                        line = m_reader.readLine();
                        if( line != null )
                        {
                            result.append( "\n" );
                        }
                    }
                    return new Object[] { result.toString() };
                }
                catch( IOException e )
                {
                    return null;
                }
            case 2:
                // close
                close();
                return null;
            case 3:
                // read
                checkOpen();
                try
                {
                    int count = optInt( args, 0, 1 );
                    if( count < 0 )
                    {
                        // Whilst this may seem absurd to allow reading 0 characters, PUC Lua it so 
                        // it seems best to remain somewhat consistent.
                        throw new LuaException( "Cannot read a negative number of characters" );
                    }
                    else if( count <= BUFFER_SIZE )
                    {
                        // If we've got a small count, then allocate that and read it.
                        char[] chars = new char[ count ];
                        int read = m_reader.read( chars );

                        return read < 0 ? null : new Object[] { new String( chars, 0, read ) };
                    }
                    else
                    {
                        // If we've got a large count, read in bunches of 8192.
                        char[] buffer = new char[ BUFFER_SIZE ];

                        // Read the initial set of characters, failing if none are read.
                        int read = m_reader.read( buffer, 0, Math.min( buffer.length, count ) );
                        if( read == -1 ) return null;

                        StringBuilder out = new StringBuilder( read );
                        count -= read;
                        out.append( buffer, 0, read );

                        // Otherwise read until we either reach the limit or we no longer consume
                        // the full buffer.
                        while( read >= BUFFER_SIZE && count > 0 )
                        {
                            read = m_reader.read( buffer, 0, Math.min( BUFFER_SIZE, count ) );
                            if( read == -1 ) break;
                            count -= read;
                            out.append( buffer, 0, read );
                        }

                        return new Object[] { out.toString() };
                    }
                }
                catch( IOException e )
                {
                    return null;
                }
            default:
                return null;
        }
    }
}
