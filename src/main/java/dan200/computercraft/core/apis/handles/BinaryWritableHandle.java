package dan200.computercraft.core.apis.handles;

import com.google.common.collect.ObjectArrays;
import dan200.computercraft.api.lua.ILuaContext;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.core.apis.ArgumentHelper;
import dan200.computercraft.shared.util.StringUtil;

import javax.annotation.Nonnull;
import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.channels.WritableByteChannel;

public class BinaryWritableHandle extends HandleGeneric
{
    private static final String[] METHOD_NAMES = new String[] { "write", "flush", "close" };
    private static final String[] METHOD_SEEK_NAMES = ObjectArrays.concat( METHOD_NAMES, new String[] { "seek" }, String.class );

    private final WritableByteChannel m_writer;
    private final SeekableByteChannel m_seekable;
    private final ByteBuffer single = ByteBuffer.allocate( 1 );

    public BinaryWritableHandle( WritableByteChannel channel, Closeable closeable )
    {
        super( closeable );
        this.m_writer = channel;
        this.m_seekable = asSeekable( channel );
    }

    public BinaryWritableHandle( WritableByteChannel channel )
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
                // write
                checkOpen();
                try
                {
                    if( args.length > 0 && args[0] instanceof Number )
                    {
                        int number = ((Number) args[0]).intValue();
                        single.clear();
                        single.put( (byte) number );
                        single.flip();

                        m_writer.write( single );
                    }
                    else if( args.length > 0 && args[0] instanceof String )
                    {
                        String value = (String) args[0];
                        m_writer.write( ByteBuffer.wrap( StringUtil.encodeString( value ) ) );
                    }
                    else
                    {
                        throw ArgumentHelper.badArgument( 0, "string or number", args.length > 0 ? args[0] : null );
                    }
                    return null;
                }
                catch( IOException e )
                {
                    throw new LuaException( e.getMessage() );
                }
            case 1:
                // flush
                checkOpen();
                try
                {
                    // Technically this is not needed
                    if( m_writer instanceof FileChannel ) ((FileChannel) m_writer).force( false );

                    return null;
                }
                catch( IOException e )
                {
                    return null;
                }
            case 2:
                //close
                close();
                return null;
            case 3:
                // seek
                checkOpen();
                return handleSeek( m_seekable, args );
            default:
                return null;
        }
    }
}
