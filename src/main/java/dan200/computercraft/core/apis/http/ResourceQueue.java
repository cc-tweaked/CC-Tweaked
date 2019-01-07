/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.core.apis.http;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

/**
 * A queue for {@link Resource}s, with built-in rate-limiting.
 */
public class ResourceQueue<T extends Resource>
{
    private static final IntSupplier ZERO = () -> 0;

    private final IntSupplier limit;

    private boolean active = false;

    private final Set<T> resources = new HashSet<>();
    private final ArrayDeque<Supplier<T>> pending = new ArrayDeque<>();

    public ResourceQueue( IntSupplier limit )
    {
        this.limit = limit;
    }

    public ResourceQueue()
    {
        this.limit = ZERO;
    }

    public void startup()
    {
        active = true;
    }

    public synchronized void shutdown()
    {
        active = false;

        pending.clear();
        for( T resource : resources ) resource.close();
        resources.clear();

        Resource.cleanup();
    }

    public void queue( T resource, Runnable setup )
    {
        queue( () -> {
            setup.run();
            return resource;
        } );
    }

    public synchronized void queue( Supplier<T> resource )
    {
        Resource.cleanup();
        if( !active ) return;

        int limit = this.limit.getAsInt();
        if( limit <= 0 || resources.size() < limit )
        {
            resources.add( resource.get() );
        }
        else
        {
            pending.add( resource );
        }
    }

    public synchronized void release( T resource )
    {
        if( !active ) return;

        resources.remove( resource );

        int limit = this.limit.getAsInt();
        if( limit <= 0 || resources.size() < limit )
        {
            Supplier<T> next = pending.poll();
            if( next != null ) resources.add( next.get() );
        }
    }
}
