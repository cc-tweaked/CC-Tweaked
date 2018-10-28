package dan200.computercraft.core.apis.handles;

import com.google.common.collect.ObjectArrays;
import dan200.computercraft.api.lua.ILuaContext;
import dan200.computercraft.api.lua.LuaException;

import javax.annotation.Nonnull;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SeekableByteChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static dan200.computercraft.core.apis.ArgumentHelper.getInt;
import static dan200.computercraft.core.apis.ArgumentHelper.optBoolean;

public class BinaryReadableHandle extends HandleGeneric
{
    private static final int BUFFER_SIZE = 8192;

    private static final String[] METHOD_NAMES = new String[] { "read", "readAll", "readLine", "close" };
    private static final String[] METHOD_SEEK_NAMES = ObjectArrays.concat( METHOD_NAMES, new String[] { "seek" }, String.class );

    private final ReadableByteChannel m_reader;
    private final SeekableByteChannel m_seekable;
    private final ByteBuffer single = ByteBuffer.allocate( 1 );

    public BinaryReadableHandle( ReadableByteChannel channel, Closeable closeable )
    {
        super( closeable );
        this.m_reader = channel;
        this.m_seekable = channel instanceof SeekableByteChannel ? (SeekableByteChannel) channel : null;
    }

    public BinaryReadableHandle( ReadableByteChannel channel )
    {
        this( channel, channel );
    }

    @Nonnull
    @Override
    public String[] getMethodNames()
    {
        return m_seekable == null ? METHOD_NAMES : METHOD_SEEK_NAMES;
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
                            throw new LuaException( "Cannot read a negative number of bytes" );
                        }
                        else if( count == 0 && m_seekable != null )
                        {
                            return m_seekable.position() >= m_seekable.size() ? null : new Object[] { "" };
                        }

                        if( count <= BUFFER_SIZE )
                        {
                            ByteBuffer buffer = ByteBuffer.allocate( count );
                            
                            int read = m_reader.read( buffer );
                            if( read < 0 ) return null;
                            return new Object[] { read < count ? Arrays.copyOf( buffer.array(), read ) : buffer.array() };
                        }
                        else
                        {
                            ByteBuffer buffer = ByteBuffer.allocate( BUFFER_SIZE );

                            int read = m_reader.read( buffer );
                            if( read < 0 ) return null;
                            int totalRead = read;

                            // If we failed to read "enough" here, let's just abort
                            if( totalRead >= count || read < BUFFER_SIZE )
                            {
                                return new Object[] { Arrays.copyOf( buffer.array(), read ) };
                            }

                            // Build up an array of ByteBuffers. Hopefully this means we can perform less allocation 
                            // than doubling up the buffer each time.
                            List<ByteBuffer> parts = new ArrayList<>( 4 );
                            parts.add( buffer );
                            while( totalRead < count && read >= BUFFER_SIZE )
                            {
                                buffer = ByteBuffer.allocate( BUFFER_SIZE );
                                totalRead += read = m_reader.read( buffer );
                                parts.add( buffer );
                            }

                            // Now just copy all the bytes across!
                            byte[] bytes = new byte[ totalRead ];
                            int pos = 0;
                            for( ByteBuffer part : parts )
                            {
                                System.arraycopy( part.array(), 0, bytes, pos, part.position() );
                                pos += part.position();
                            }
                            return new Object[] { bytes };
                        }
                    }
                    else
                    {
                        single.clear();
                        int b = m_reader.read( single );
                        return b == -1 ? null : new Object[] { single.get( 0 ) & 0xFF };
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
                    int expected = 32;
                    if( m_seekable != null )
                    {
                        expected = Math.max( expected, (int) (m_seekable.size() - m_seekable.position()) );
                    }
                    ByteArrayOutputStream stream = new ByteArrayOutputStream( expected );

                    ByteBuffer buf = ByteBuffer.allocate( 8192 );
                    boolean readAnything = false;
                    while( true )
                    {
                        buf.clear();
                        int r = m_reader.read( buf );
                        if( r == -1 ) break;

                        readAnything = true;
                        stream.write( buf.array(), 0, r );
                    }
                    return readAnything ? new Object[] { stream.toByteArray() } : null;
                }
                catch( IOException e )
                {
                    return null;
                }
            case 2:
            {
                // readLine
                checkOpen();
                boolean withTrailing = optBoolean( args, 0, false );
                try
                {
                    ByteArrayOutputStream stream = new ByteArrayOutputStream();

                    boolean readAnything = false;
                    while( true )
                    {
                        single.clear();
                        int r = m_reader.read( single );
                        if( r == -1 ) break;

                        readAnything = true;
                        byte b = single.get( 0 );
                        if( b == '\n' )
                        {
                            if( withTrailing ) stream.write( b );
                            break;
                        }
                        else
                        {
                            stream.write( b );
                        }
                    }

                    return readAnything ? new Object[] { stream.toByteArray() } : null;
                }
                catch( IOException e )
                {
                    return null;
                }
            }
            case 3:
                //close
                close();
                return null;
            case 4:
                // seek
                checkOpen();
                return handleSeek( m_seekable, args );
            default:
                return null;
        }
    }
}
