/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.peripheral.generic.methods;

import dan200.computercraft.api.lua.LuaException;
import net.minecraft.ResourceLocationException;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.IForgeRegistryEntry;

import javax.annotation.Nonnull;

/**
 * A few helpers for working with arguments.
 *
 * This should really be moved into the public API. However, until I have settled on a suitable format, we'll keep it
 * where it is used.
 */
final class ArgumentHelpers
{
    private ArgumentHelpers()
    {
    }

    public static void assertBetween( double value, double min, double max, String message ) throws LuaException
    {
        if( value < min || value > max || Double.isNaN( value ) )
        {
            throw new LuaException( String.format( message, "between " + min + " and " + max ) );
        }
    }

    public static void assertBetween( int value, int min, int max, String message ) throws LuaException
    {
        if( value < min || value > max )
        {
            throw new LuaException( String.format( message, "between " + min + " and " + max ) );
        }
    }

    @Nonnull
    public static <T extends IForgeRegistryEntry<T>> T getRegistryEntry( String name, String typeName, IForgeRegistry<T> registry ) throws LuaException
    {
        ResourceLocation id;
        try
        {
            id = new ResourceLocation( name );
        }
        catch( ResourceLocationException e )
        {
            id = null;
        }

        T value;
        if( id == null || !registry.containsKey( id ) || (value = registry.getValue( id )) == null )
        {
            throw new LuaException( String.format( "Unknown %s '%s'", typeName, name ) );
        }

        return value;
    }
}
