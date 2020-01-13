/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.core.apis;

import dan200.computercraft.api.lua.LuaException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;

/**
 * A stub for any mods which depended on this version of the argument helper.
 *
 * @deprecated Use {@link dan200.computercraft.api.lua.ArgumentHelper}.
 */
@Deprecated
public final class ArgumentHelper
{
    private ArgumentHelper()
    {
    }

    @Nonnull
    public static String getType( @Nullable Object type )
    {
        return dan200.computercraft.api.lua.ArgumentHelper.getType( type );
    }

    @Nonnull
    public static LuaException badArgument( int index, @Nonnull String expected, @Nullable Object actual )
    {
        return dan200.computercraft.api.lua.ArgumentHelper.badArgumentOf( index, expected, actual );
    }

    @Nonnull
    public static LuaException badArgument( int index, @Nonnull String expected, @Nonnull String actual )
    {
        return dan200.computercraft.api.lua.ArgumentHelper.badArgument( index, expected, actual );
    }

    public static double getNumber( @Nonnull Object[] args, int index ) throws LuaException
    {
        return dan200.computercraft.api.lua.ArgumentHelper.getDouble( args, index );
    }

    public static int getInt( @Nonnull Object[] args, int index ) throws LuaException
    {
        return dan200.computercraft.api.lua.ArgumentHelper.getInt( args, index );
    }

    public static long getLong( @Nonnull Object[] args, int index ) throws LuaException
    {
        return dan200.computercraft.api.lua.ArgumentHelper.getLong( args, index );
    }

    public static double getReal( @Nonnull Object[] args, int index ) throws LuaException
    {
        return dan200.computercraft.api.lua.ArgumentHelper.getFiniteDouble( args, index );
    }

    public static boolean getBoolean( @Nonnull Object[] args, int index ) throws LuaException
    {
        return dan200.computercraft.api.lua.ArgumentHelper.getBoolean( args, index );
    }

    @Nonnull
    public static String getString( @Nonnull Object[] args, int index ) throws LuaException
    {
        return dan200.computercraft.api.lua.ArgumentHelper.getString( args, index );
    }

    @Nonnull
    @SuppressWarnings( "unchecked" )
    public static Map<Object, Object> getTable( @Nonnull Object[] args, int index ) throws LuaException
    {
        return (Map<Object, Object>) dan200.computercraft.api.lua.ArgumentHelper.getTable( args, index );
    }

    public static double optNumber( @Nonnull Object[] args, int index, double def ) throws LuaException
    {
        return dan200.computercraft.api.lua.ArgumentHelper.optDouble( args, index, def );
    }

    public static int optInt( @Nonnull Object[] args, int index, int def ) throws LuaException
    {
        return dan200.computercraft.api.lua.ArgumentHelper.optInt( args, index, def );
    }

    public static long optLong( @Nonnull Object[] args, int index, long def ) throws LuaException
    {
        return dan200.computercraft.api.lua.ArgumentHelper.optLong( args, index, def );
    }

    public static double optReal( @Nonnull Object[] args, int index, double def ) throws LuaException
    {
        return dan200.computercraft.api.lua.ArgumentHelper.optFiniteDouble( args, index, def );
    }

    public static boolean optBoolean( @Nonnull Object[] args, int index, boolean def ) throws LuaException
    {
        return dan200.computercraft.api.lua.ArgumentHelper.optBoolean( args, index, def );
    }

    public static String optString( @Nonnull Object[] args, int index, String def ) throws LuaException
    {
        return dan200.computercraft.api.lua.ArgumentHelper.optString( args, index, def );
    }

    @SuppressWarnings( "unchecked" )
    public static Map<Object, Object> optTable( @Nonnull Object[] args, int index, Map<Object, Object> def ) throws LuaException
    {
        return (Map<Object, Object>) dan200.computercraft.api.lua.ArgumentHelper.optTable( args, index, def );
    }
}
