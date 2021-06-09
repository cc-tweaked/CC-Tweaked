/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.core.apis.http;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

/**
 * A collection of {@link Resource}s, with an upper bound on capacity.
 *
 * @param <T> The type of the resource this group manages.
 */
public class ResourceGroup<T extends Resource<T>>
{
    private static final IntSupplier ZERO = () -> 0;

    final IntSupplier limit;
    final Set<T> resources = Collections.newSetFromMap( new ConcurrentHashMap<>() );
    boolean active = false;

    public ResourceGroup( IntSupplier limit )
    {
        this.limit = limit;
    }

    public ResourceGroup()
    {
        this.limit = ZERO;
    }

    public void startup()
    {
        this.active = true;
    }

    public synchronized void shutdown()
    {
        this.active = false;

        for( T resource : this.resources )
        {
            resource.close();
        }
        this.resources.clear();

        Resource.cleanup();
    }


    public final boolean queue( T resource, Runnable setup )
    {
        return this.queue( () -> {
            setup.run();
            return resource;
        } );
    }

    public synchronized boolean queue( Supplier<T> resource )
    {
        Resource.cleanup();
        if( !this.active )
        {
            return false;
        }

        int limit = this.limit.getAsInt();
        if( limit <= 0 || this.resources.size() < limit )
        {
            this.resources.add( resource.get() );
            return true;
        }

        return false;
    }

    public synchronized void release( T resource )
    {
        this.resources.remove( resource );
    }
}
