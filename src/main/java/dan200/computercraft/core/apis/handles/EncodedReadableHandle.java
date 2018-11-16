package dan200.computercraft.core.apis.handles;

import dan200.computercraft.api.lua.ILuaContext;
import dan200.computercraft.api.lua.LuaException;

import javax.annotation.Nonnull;
import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;

import static dan200.computercraft.core.apis.ArgumentHelper.optBoolean;
import static dan200.computercraft.core.apis.ArgumentHelper.optInt;

public class EncodedReadableHandle extends HandleGeneric
{
    private static final int BUFFER_SIZE = 8192;

    private BufferedReader m_reader;

    public EncodedReadableHandle( @Nonnull BufferedReader reader, @Nonnull Closeable closable )
    {
        super( closable );
        this.m_reader = reader;
    }

    public EncodedReadableHandle( @Nonnull BufferedReader reader )
    {
        this( reader, reader );
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
            {
                // readLine
                checkOpen();
                boolean withTrailing = optBoolean( args, 0, false );
                try
                {
                    String line = m_reader.readLine();
                    if( line != null )
                    {
                        // While this is technically inaccurate, it's better than nothing
                        if( withTrailing ) line += "\n";
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

    public static BufferedReader openUtf8( ReadableByteChannel channel )
    {
        return open( channel, StandardCharsets.UTF_8 );
    }

    public static BufferedReader open( ReadableByteChannel channel, Charset charset )
    {
        // Create a charset decoder with the same properties as StreamDecoder does for 
        // InputStreams: namely, replace everything instead of erroring.
        CharsetDecoder decoder = charset.newDecoder()
            .onMalformedInput( CodingErrorAction.REPLACE )
            .onUnmappableCharacter( CodingErrorAction.REPLACE );
        return new BufferedReader( Channels.newReader( channel, decoder, -1 ) );
    }
}
