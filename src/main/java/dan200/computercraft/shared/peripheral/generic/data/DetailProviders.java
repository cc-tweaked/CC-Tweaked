/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.peripheral.generic.data;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.api.detail.IBlockDetailProvider;
import dan200.computercraft.api.detail.IDetailProvider;
import dan200.computercraft.api.detail.IFluidDetailProvider;
import dan200.computercraft.api.detail.IItemDetailProvider;
import net.minecraft.block.BlockState;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import java.util.*;

public final class DetailProviders
{
    private static final Map<Class<?>, Collection<IDetailProvider<?>>> allProviders = new HashMap<>();

    public static synchronized <T> void register( IDetailProvider<T> provider )
    {
        Objects.requireNonNull( provider, "provider cannot be null" );

        // A detail provider can implement multiple interfaces. Check the valid ones, and log an error if the given
        // provider matched none of them.
        boolean matched = false;

        if ( provider instanceof IBlockDetailProvider )
        {
            registerProvider( BlockState.class, (IBlockDetailProvider) provider );
            matched = true;
        }

        if ( provider instanceof IFluidDetailProvider )
        {
            registerProvider( FluidStack.class, (IFluidDetailProvider) provider );
            matched = true;
        }

        if ( provider instanceof IItemDetailProvider )
        {
            registerProvider( ItemStack.class, (IItemDetailProvider) provider );
            matched = true;
        }

        if ( !matched )
        {
            ComputerCraft.log.error( "Detail provider {} does not implement any valid interfaces.", provider.getClass().getName() );
        }
    }

    private static <T> void registerProvider( Class<T> type, IDetailProvider<T> provider )
    {
        allProviders.computeIfAbsent( type, k -> new LinkedHashSet<>() ).add( provider );
    }

    @SuppressWarnings( "unchecked" )
    public static <T> void fillData( Class<T> type, Map<? super String, Object> data, T value )
    {
        Collection<IDetailProvider<T>> providers = (Collection<IDetailProvider<T>>) (Object) allProviders.get( type );
        if ( providers == null ) return;

        for ( IDetailProvider<T> provider : providers )
        {
            try
            {
                provider.provideDetails( data, value );
            }
            catch ( Exception e )
            {
                ComputerCraft.log.error( "Error while providing details for {}", value, e );
                // TODO: Should this throw an exception here, preventing the rest of the details from being filled?
            }
        }
    }
}
