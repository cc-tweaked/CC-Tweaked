/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.peripheral.generic.data;

import dan200.computercraft.api.detail.BlockReference;
import dan200.computercraft.api.detail.IDetailProvider;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import java.util.*;

public final class DetailProviders
{
    private static final Map<Class<?>, Collection<IDetailProvider<?>>> allProviders = new HashMap<>();

    public static synchronized <T> void registerProvider( Class<T> type, IDetailProvider<T> provider )
    {
        Objects.requireNonNull( type, "type cannot be null" );
        Objects.requireNonNull( provider, "provider cannot be null" );

        if( type != BlockReference.class && type != ItemStack.class && type != FluidStack.class )
        {
            throw new IllegalArgumentException( "type must be assignable from BlockReference, ItemStack or FluidStack" );
        }

        allProviders.computeIfAbsent( type, k -> new LinkedHashSet<>() ).add( provider );
    }

    @SuppressWarnings( "unchecked" )
    public static <T> void fillData( Class<T> type, Map<? super String, Object> data, T value )
    {
        Collection<IDetailProvider<T>> providers = (Collection<IDetailProvider<T>>) (Object) allProviders.get( type );
        if( providers == null ) return;

        for( IDetailProvider<T> provider : providers )
        {
            provider.provideDetails( data, value );
        }
    }
}
