/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.core.apis.http;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class MonitorerdResource implements Closeable
{
    private final AtomicBoolean closed = new AtomicBoolean( false );

    /**
     * Whether this resource is closed.
     *
     * @return Whether this resource is closed.
     */
    public final boolean isClosed()
    {
        return closed.get();
    }

    /**
     * Checks if this has been cancelled. If so, it'll clean up any
     * existing resources and cancel any pending futures.
     */
    public final boolean checkClosed()
    {
        if( !closed.get() ) return false;
        dispose();
        return true;
    }

    /**
     * Try to close the current resource.
     *
     * @return Whether this has not been closed before.
     */
    public final boolean tryClose()
    {
        if( closed.getAndSet( true ) ) return false;
        dispose();
        return true;
    }

    /**
     * Clean up any pending resources
     *
     * Note, this may be called multiple times, and so should be thread-safe and
     * avoid any major side effects.
     */
    protected abstract void dispose();

    @Override
    public final void close()
    {
        tryClose();
    }

    protected static <T extends Closeable> T closeCloseable( T closeable )
    {
        if( closeable != null )
        {
            try
            {
                closeable.close();
            }
            catch( IOException ignored )
            {
            }
        }

        return null;
    }

    protected static ChannelFuture closeChannel( ChannelFuture future )
    {
        if( future != null )
        {
            future.cancel( false );
            Channel channel = future.channel();
            if( channel != null && channel.isOpen() ) channel.close();
        }

        return null;
    }

    protected static <T extends Future<?>> T closeFuture( T future )
    {
        if( future != null ) future.cancel( true );
        return null;
    }
}
