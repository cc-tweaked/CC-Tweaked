package dan200.computercraft.core.apis.handles;

import com.google.common.io.ByteStreams;
import dan200.computercraft.api.lua.ILuaContext;
import dan200.computercraft.api.lua.LuaException;

import javax.annotation.Nonnull;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import static dan200.computercraft.core.apis.ArgumentHelper.getInt;

public class BinaryInputHandle extends HandleGeneric
{
    private static final int BUFFER_SIZE = 8192;

    private final InputStream m_stream;

    public BinaryInputHandle( InputStream reader )
    {
        super( reader );
        this.m_stream = reader;
    }

    @Nonnull
    @Override
    public String[] getMethodNames()
    {
        return new String[] {
            "read",
            "readAll",
            "close",
        };
    }

    @Override
    public Object[] callMethod( @Nonnull ILuaContext context, int method, @Nonnull Object[] args ) throws LuaException
    {
        switch( method )
        {
            case 0:
                // read
                checkOpen();
                try
                {
                    if( args.length > 0 && args[ 0 ] != null )
                    {
                        int count = getInt( args, 0 );
                        if( count < 0 )
                        {
                            // Whilst this may seem absurd to allow reading 0 bytes, PUC Lua it so 
                            // it seems best to remain somewhat consistent.
                            throw new LuaException( "Cannot read a negative number of bytes" );
                        }
                        else if( count <= BUFFER_SIZE )
                        {
                            // If we've got a small count, then allocate that and read it.
                            byte[] bytes = new byte[ count ];
                            int read = m_stream.read( bytes );

                            if( read < 0 ) return null;
                            if( read < count ) bytes = Arrays.copyOf( bytes, read );
                            return new Object[] { bytes };
                        }
                        else
                        {
                            byte[] buffer = new byte[ BUFFER_SIZE ];

                            // Read the initial set of bytes, failing if none are read.
                            int read = m_stream.read( buffer, 0, Math.min( buffer.length, count ) );
                            if( read == -1 ) return null;

                            ByteArrayOutputStream out = new ByteArrayOutputStream( read );
                            count -= read;
                            out.write( buffer, 0, read );

                            // Otherwise read until we either reach the limit or we no longer consume
                            // the full buffer.
                            while( read >= buffer.length && count > 0 )
                            {
                                read = m_stream.read( buffer, 0, Math.min( BUFFER_SIZE, count ) );
                                if( read == -1 ) break;
                                count -= read;
                                out.write( buffer, 0, read );
                            }

                            return new Object[] { out.toByteArray() };
                        }
                    }
                    else
                    {
                        int b = m_stream.read();
                        return b == -1 ? null : new Object[] { b };
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
                    byte[] out = ByteStreams.toByteArray( m_stream );
                    return out == null ? null : new Object[] { out };
                }
                catch( IOException e )
                {
                    return null;
                }
            case 2:
                //close
                close();
                return null;
            default:
                return null;
        }
    }
}
