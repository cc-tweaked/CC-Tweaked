/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.impl.detail;

import dan200.computercraft.api.detail.DetailRegistry;
import dan200.computercraft.api.detail.IDetailProvider;

import java.util.*;

/**
 * Concrete implementation of {@link DetailRegistry}.
 *
 * @param <T> The type of object that this registry provides details for.
 */
public class DetailRegistryImpl<T> implements DetailRegistry<T>
{
    private final Collection<IDetailProvider<T>> providers = new ArrayList<>();
    private final IDetailProvider<T> basic;

    public DetailRegistryImpl( IDetailProvider<T> basic )
    {
        this.basic = basic;
        providers.add( basic );
    }

    @Override
    public synchronized void addProvider( IDetailProvider<T> provider )
    {
        Objects.requireNonNull( provider, "provider cannot be null" );
        if( !providers.contains( provider ) ) providers.add( provider );
    }

    @Override
    public Map<String, Object> getBasicDetails( T object )
    {
        Objects.requireNonNull( object, "object cannot be null" );

        Map<String, Object> map = new HashMap<>( 4 );
        basic.provideDetails( map, object );
        return map;
    }

    @Override
    public Map<String, Object> getDetails( T object )
    {
        Objects.requireNonNull( object, "object cannot be null" );

        Map<String, Object> map = new HashMap<>();
        for( IDetailProvider<T> provider : providers ) provider.provideDetails( map, object );
        return map;
    }
}
