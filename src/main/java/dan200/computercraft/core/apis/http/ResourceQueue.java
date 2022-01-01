/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.core.apis.http;

import java.util.ArrayDeque;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

/**
 * A {@link ResourceGroup} which will queue items when the group at capacity.
 *
 * @param <T> The type of the resource this queue manages.
 */
public class ResourceQueue<T extends Resource<T>> extends ResourceGroup<T>
{
    private final ArrayDeque<Supplier<T>> pending = new ArrayDeque<>();

    public ResourceQueue( IntSupplier limit )
    {
        super( limit );
    }

    public ResourceQueue()
    {
    }

    @Override
    public synchronized void shutdown()
    {
        super.shutdown();
        pending.clear();
    }

    @Override
    public synchronized boolean queue( Supplier<T> resource )
    {
        if( !active ) return false;
        if( super.queue( resource ) ) return true;
        if( pending.size() > DEFAULT_LIMIT ) return false;

        pending.add( resource );
        return true;
    }

    @Override
    public synchronized void release( T resource )
    {
        super.release( resource );

        if( !active ) return;

        int limit = this.limit.getAsInt();
        if( limit <= 0 || resources.size() < limit )
        {
            Supplier<T> next = pending.poll();
            if( next != null ) resources.add( next.get() );
        }
    }
}
