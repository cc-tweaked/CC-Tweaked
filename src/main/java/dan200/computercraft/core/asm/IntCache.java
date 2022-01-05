/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.core.asm;

import java.util.Arrays;
import java.util.function.IntFunction;

public final class IntCache<T>
{
    private final IntFunction<T> factory;
    private volatile Object[] cache = new Object[16];

    IntCache( IntFunction<T> factory )
    {
        this.factory = factory;
    }

    @SuppressWarnings( "unchecked" )
    public T get( int index )
    {
        if( index < 0 ) throw new IllegalArgumentException( "index < 0" );

        if( index < cache.length )
        {
            T current = (T) cache[index];
            if( current != null ) return current;
        }

        synchronized( this )
        {
            if( index >= cache.length ) cache = Arrays.copyOf( cache, Math.max( cache.length * 2, index + 1 ) );
            T current = (T) cache[index];
            if( current == null ) cache[index] = current = factory.apply( index );
            return current;
        }
    }
}
