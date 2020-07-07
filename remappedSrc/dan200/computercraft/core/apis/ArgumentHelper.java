/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.core.apis;

import dan200.computercraft.api.lua.LuaException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;

/**
 * Various helpers for arguments
 */
public final class ArgumentHelper
{
    private ArgumentHelper()
    {
        throw new IllegalStateException( "Cannot instantiate singleton " + getClass().getName() );
    }

    @Nonnull
    public static String getType( @Nullable Object type )
    {
        if( type == null ) return "nil";
        if( type instanceof String ) return "string";
        if( type instanceof Boolean ) return "boolean";
        if( type instanceof Number ) return "number";
        if( type instanceof Map ) return "table";

        Class<?> klass = type.getClass();
        if( klass.isArray() )
        {
            StringBuilder name = new StringBuilder();
            while( klass.isArray() )
            {
                name.append( "[]" );
                klass = klass.getComponentType();
            }
            name.insert( 0, klass.getName() );
            return name.toString();
        }
        else
        {
            return klass.getName();
        }
    }

    @Nonnull
    public static LuaException badArgument( int index, @Nonnull String expected, @Nullable Object actual )
    {
        return badArgument( index, expected, getType( actual ) );
    }

    @Nonnull
    public static LuaException badArgument( int index, @Nonnull String expected, @Nonnull String actual )
    {
        return new LuaException( "bad argument #" + (index + 1) + " (" + expected + " expected, got " + actual + ")" );
    }

    public static double getNumber( @Nonnull Object[] args, int index ) throws LuaException
    {
        if( index >= args.length ) throw badArgument( index, "number", "nil" );
        Object value = args[index];
        if( value instanceof Number )
        {
            return ((Number) value).doubleValue();
        }
        else
        {
            throw badArgument( index, "number", value );
        }
    }

    public static int getInt( @Nonnull Object[] args, int index ) throws LuaException
    {
        return (int) getLong( args, index );
    }

    public static long getLong( @Nonnull Object[] args, int index ) throws LuaException
    {
        if( index >= args.length ) throw badArgument( index, "number", "nil" );
        Object value = args[index];
        if( value instanceof Number )
        {
            return checkReal( index, (Number) value ).longValue();
        }
        else
        {
            throw badArgument( index, "number", value );
        }
    }

    public static double getReal( @Nonnull Object[] args, int index ) throws LuaException
    {
        return checkReal( index, getNumber( args, index ) );
    }

    public static boolean getBoolean( @Nonnull Object[] args, int index ) throws LuaException
    {
        if( index >= args.length ) throw badArgument( index, "boolean", "nil" );
        Object value = args[index];
        if( value instanceof Boolean )
        {
            return (Boolean) value;
        }
        else
        {
            throw badArgument( index, "boolean", value );
        }
    }

    @Nonnull
    public static String getString( @Nonnull Object[] args, int index ) throws LuaException
    {
        if( index >= args.length ) throw badArgument( index, "string", "nil" );
        Object value = args[index];
        if( value instanceof String )
        {
            return (String) value;
        }
        else
        {
            throw badArgument( index, "string", value );
        }
    }

    @SuppressWarnings( "unchecked" )
    @Nonnull
    public static Map<Object, Object> getTable( @Nonnull Object[] args, int index ) throws LuaException
    {
        if( index >= args.length ) throw badArgument( index, "table", "nil" );
        Object value = args[index];
        if( value instanceof Map )
        {
            return (Map<Object, Object>) value;
        }
        else
        {
            throw badArgument( index, "table", value );
        }
    }

    public static double optNumber( @Nonnull Object[] args, int index, double def ) throws LuaException
    {
        Object value = index < args.length ? args[index] : null;
        if( value == null )
        {
            return def;
        }
        else if( value instanceof Number )
        {
            return ((Number) value).doubleValue();
        }
        else
        {
            throw badArgument( index, "number", value );
        }
    }

    public static int optInt( @Nonnull Object[] args, int index, int def ) throws LuaException
    {
        return (int) optLong( args, index, def );
    }

    public static long optLong( @Nonnull Object[] args, int index, long def ) throws LuaException
    {
        Object value = index < args.length ? args[index] : null;
        if( value == null )
        {
            return def;
        }
        else if( value instanceof Number )
        {
            return checkReal( index, (Number) value ).longValue();
        }
        else
        {
            throw badArgument( index, "number", value );
        }
    }

    public static double optReal( @Nonnull Object[] args, int index, double def ) throws LuaException
    {
        return checkReal( index, optNumber( args, index, def ) );
    }

    public static boolean optBoolean( @Nonnull Object[] args, int index, boolean def ) throws LuaException
    {
        Object value = index < args.length ? args[index] : null;
        if( value == null )
        {
            return def;
        }
        else if( value instanceof Boolean )
        {
            return (Boolean) value;
        }
        else
        {
            throw badArgument( index, "boolean", value );
        }
    }

    public static String optString( @Nonnull Object[] args, int index, String def ) throws LuaException
    {
        Object value = index < args.length ? args[index] : null;
        if( value == null )
        {
            return def;
        }
        else if( value instanceof String )
        {
            return (String) value;
        }
        else
        {
            throw badArgument( index, "string", value );
        }
    }

    @SuppressWarnings( "unchecked" )
    public static Map<Object, Object> optTable( @Nonnull Object[] args, int index, Map<Object, Object> def ) throws LuaException
    {
        Object value = index < args.length ? args[index] : null;
        if( value == null )
        {
            return def;
        }
        else if( value instanceof Map )
        {
            return (Map<Object, Object>) value;
        }
        else
        {
            throw badArgument( index, "table", value );
        }
    }

    private static Number checkReal( int index, Number value ) throws LuaException
    {
        checkReal( index, value.doubleValue() );
        return value;
    }

    private static double checkReal( int index, double value ) throws LuaException
    {
        if( Double.isNaN( value ) )
        {
            throw badArgument( index, "number", "nan" );
        }
        else if( value == Double.POSITIVE_INFINITY )
        {
            throw badArgument( index, "number", "inf" );
        }
        else if( value == Double.NEGATIVE_INFINITY )
        {
            throw badArgument( index, "number", "-inf" );
        }
        else
        {
            return value;
        }
    }
}
