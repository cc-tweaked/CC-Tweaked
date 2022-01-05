/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.core.apis.http;

import dan200.computercraft.shared.util.IoUtil;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;

import java.io.Closeable;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * A holder for one or more resources, with a lifetime.
 *
 * @param <T> The type of this resource. Should be the class extending from {@link Resource}.
 */
public abstract class Resource<T extends Resource<T>> implements Closeable
{
    private final AtomicBoolean closed = new AtomicBoolean( false );
    private final ResourceGroup<T> limiter;

    protected Resource( ResourceGroup<T> limiter )
    {
        this.limiter = limiter;
    }

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
     * Checks if this has been cancelled. If so, it'll clean up any existing resources and cancel any pending futures.
     *
     * @return Whether this resource has been closed.
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
     * @return Whether this was successfully closed, or {@code false} if it has already been closed.
     */
    protected final boolean tryClose()
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
    protected void dispose()
    {
        @SuppressWarnings( "unchecked" )
        T thisT = (T) this;
        limiter.release( thisT );
    }

    /**
     * Create a {@link WeakReference} which will close {@code this} when collected.
     *
     * @param <R>    The object we are wrapping in a reference.
     * @param object The object to reference to
     * @return The weak reference.
     */
    protected <R> WeakReference<R> createOwnerReference( R object )
    {
        return new CloseReference<>( this, object );
    }

    @Override
    public final void close()
    {
        tryClose();
    }

    public final boolean queue( Consumer<T> task )
    {
        @SuppressWarnings( "unchecked" )
        T thisT = (T) this;
        return limiter.queue( thisT, () -> task.accept( thisT ) );
    }

    protected static <T extends Closeable> T closeCloseable( T closeable )
    {
        IoUtil.closeQuietly( closeable );
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


    private static final ReferenceQueue<Object> QUEUE = new ReferenceQueue<>();

    private static class CloseReference<T> extends WeakReference<T>
    {
        final Resource<?> resource;

        CloseReference( Resource<?> resource, T referent )
        {
            super( referent, QUEUE );
            this.resource = resource;
        }
    }

    public static void cleanup()
    {
        Reference<?> reference;
        while( (reference = QUEUE.poll()) != null ) ((CloseReference<?>) reference).resource.close();
    }
}
