package dan200.computercraft.core.apis.handles;

import com.google.common.base.Preconditions;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.NonWritableChannelException;
import java.nio.channels.SeekableByteChannel;

/**
 * A seekable, readable byte channel which is backed by a simple byte array.
 */
public class ArrayByteChannel implements SeekableByteChannel
{
    private boolean closed = false;
    private int position = 0;

    private final byte[] backing;

    public ArrayByteChannel( byte[] backing )
    {
        this.backing = backing;
    }

    @Override
    public int read( ByteBuffer destination ) throws IOException
    {
        if( closed ) throw new ClosedChannelException();
        Preconditions.checkNotNull( destination, "destination" );

        if( position >= backing.length ) return -1;

        int remaining = Math.min( backing.length - position, destination.remaining() );
        destination.put( backing, position, remaining );
        position += remaining;
        return remaining;
    }

    @Override
    public int write( ByteBuffer src ) throws IOException
    {
        if( closed ) throw new ClosedChannelException();
        throw new NonWritableChannelException();
    }

    @Override
    public long position() throws IOException
    {
        if( closed ) throw new ClosedChannelException();
        return position;
    }

    @Override
    public SeekableByteChannel position( long newPosition ) throws IOException
    {
        if( closed ) throw new ClosedChannelException();
        if( newPosition < 0 || newPosition > Integer.MAX_VALUE )
        {
            throw new IllegalArgumentException( "Position out of bounds" );
        }
        position = (int) newPosition;
        return this;
    }

    @Override
    public long size() throws IOException
    {
        if( closed ) throw new ClosedChannelException();
        return backing.length;
    }

    @Override
    public SeekableByteChannel truncate( long size ) throws IOException
    {
        if( closed ) throw new ClosedChannelException();
        throw new NonWritableChannelException();
    }

    @Override
    public boolean isOpen()
    {
        return !closed;
    }

    @Override
    public void close()
    {
        closed = true;
    }
}
