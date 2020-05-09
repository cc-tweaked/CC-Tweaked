/*
 * This file is part of the public ComputerCraft API - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. This API may be redistributed unmodified and in full only.
 * For help using the API, and posting your mods, visit the forums at computercraft.info.
 */
package dan200.computercraft.api.lua;

import dan200.computercraft.api.peripheral.IComputerAccess;
import dan200.computercraft.api.peripheral.IDynamicPeripheral;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;

/**
 * Provides methods for extracting values and validating Lua arguments, such as those provided to {@link LuaFunction}
 * or {@link IDynamicPeripheral#callMethod(IComputerAccess, ILuaContext, int, Object[])}
 *
 * This provides two sets of functions: the {@code get*} methods, which require an argument to be valid, and
 * {@code opt*}, which accept a default value and return that if the argument was not present or was {@code null}.
 * If the argument is of the wrong type, a suitable error message will be thrown, with a similar format to Lua's own
 * error messages.
 *
 * <h2>Example usage:</h2>
 * <pre>
 * {@code
 * int slot = getInt( args, 0 );
 * int amount = optInt( args, 1, 64 );
 * }
 * </pre>
 */
public final class ArgumentHelper
{
    private ArgumentHelper()
    {
    }

    /**
     * Get a string representation of the given value's type.
     *
     * @param value The value whose type we are trying to compute.
     * @return A string representation of the given value's type, in a similar format to that provided by Lua's
     * {@code type} function.
     */
    @Nonnull
    public static String getType( @Nullable Object value )
    {
        if( value == null ) return "nil";
        if( value instanceof String ) return "string";
        if( value instanceof Boolean ) return "boolean";
        if( value instanceof Number ) return "number";
        if( value instanceof Map ) return "table";
        return "userdata";
    }

    /**
     * Construct a "bad argument" exception, from an expected type and the actual value provided.
     *
     * @param index    The argument number, starting from 0.
     * @param expected The expected type for this argument.
     * @param actual   The actual value provided for this argument.
     * @return The constructed exception, which should be thrown immediately.
     */
    @Nonnull
    public static LuaException badArgumentOf( int index, @Nonnull String expected, @Nullable Object actual )
    {
        return badArgument( index, expected, getType( actual ) );
    }

    /**
     * Construct a "bad argument" exception, from an expected and actual type.
     *
     * @param index    The argument number, starting from 0.
     * @param expected The expected type for this argument.
     * @param actual   The provided type for this argument.
     * @return The constructed exception, which should be thrown immediately.
     */
    @Nonnull
    public static LuaException badArgument( int index, @Nonnull String expected, @Nonnull String actual )
    {
        return new LuaException( "bad argument #" + (index + 1) + " (" + expected + " expected, got " + actual + ")" );
    }

    /**
     * Get an argument as a double.
     *
     * @param args  The arguments to extract from.
     * @param index The index into the argument array to read from.
     * @return The argument's value.
     * @throws LuaException If the value is not a number.
     * @see #getFiniteDouble(Object[], int) if you require this to be finite (i.e. not infinite or NaN).
     */
    public static double getDouble( @Nonnull Object[] args, int index ) throws LuaException
    {
        if( index >= args.length ) throw badArgument( index, "number", "nil" );
        Object value = args[index];
        if( !(value instanceof Number) ) throw badArgumentOf( index, "number", value );
        return ((Number) value).doubleValue();
    }

    /**
     * Get an argument as an integer.
     *
     * @param args  The arguments to extract from.
     * @param index The index into the argument array to read from.
     * @return The argument's value.
     * @throws LuaException If the value is not an integer.
     */
    public static int getInt( @Nonnull Object[] args, int index ) throws LuaException
    {
        return (int) getLong( args, index );
    }

    /**
     * Get an argument as a long.
     *
     * @param args  The arguments to extract from.
     * @param index The index into the argument array to read from.
     * @return The argument's value.
     * @throws LuaException If the value is not a long.
     */
    public static long getLong( @Nonnull Object[] args, int index ) throws LuaException
    {
        if( index >= args.length ) throw badArgument( index, "number", "nil" );
        Object value = args[index];
        if( !(value instanceof Number) ) throw badArgumentOf( index, "number", value );
        return checkFinite( index, (Number) value ).longValue();
    }

    /**
     * Get an argument as a finite number (not infinite or NaN).
     *
     * @param args  The arguments to extract from.
     * @param index The index into the argument array to read from.
     * @return The argument's value.
     * @throws LuaException If the value is not finite.
     */
    public static double getFiniteDouble( @Nonnull Object[] args, int index ) throws LuaException
    {
        return checkFinite( index, getDouble( args, index ) );
    }

    /**
     * Get an argument as a boolean.
     *
     * @param args  The arguments to extract from.
     * @param index The index into the argument array to read from.
     * @return The argument's value.
     * @throws LuaException If the value is not a boolean.
     */
    public static boolean getBoolean( @Nonnull Object[] args, int index ) throws LuaException
    {
        if( index >= args.length ) throw badArgument( index, "boolean", "nil" );
        Object value = args[index];
        if( !(value instanceof Boolean) ) throw badArgumentOf( index, "boolean", value );
        return (Boolean) value;
    }

    /**
     * Get an argument as a string.
     *
     * @param args  The arguments to extract from.
     * @param index The index into the argument array to read from.
     * @return The argument's value.
     * @throws LuaException If the value is not a string.
     */
    @Nonnull
    public static String getString( @Nonnull Object[] args, int index ) throws LuaException
    {
        if( index >= args.length ) throw badArgument( index, "string", "nil" );
        Object value = args[index];
        if( !(value instanceof String) ) throw badArgumentOf( index, "string", value );
        return (String) value;
    }

    /**
     * Get an argument as a table.
     *
     * @param args  The arguments to extract from.
     * @param index The index into the argument array to read from.
     * @return The argument's value.
     * @throws LuaException If the value is not a table.
     */
    @Nonnull
    public static Map<?, ?> getTable( @Nonnull Object[] args, int index ) throws LuaException
    {
        if( index >= args.length ) throw badArgument( index, "table", "nil" );
        Object value = args[index];
        if( !(value instanceof Map) ) throw badArgumentOf( index, "table", value );
        return (Map<?, ?>) value;
    }

    /**
     * Get an argument as a double.
     *
     * @param args  The arguments to extract from.
     * @param index The index into the argument array to read from.
     * @param def   The default value, if this argument is not given.
     * @return The argument's value, or {@code def} if none was provided.
     * @throws LuaException If the value is not a number.
     */
    public static double optDouble( @Nonnull Object[] args, int index, double def ) throws LuaException
    {
        Object value = index < args.length ? args[index] : null;
        if( value == null ) return def;
        if( !(value instanceof Number) ) throw badArgumentOf( index, "number", value );
        return ((Number) value).doubleValue();
    }

    /**
     * Get an argument as an int.
     *
     * @param args  The arguments to extract from.
     * @param index The index into the argument array to read from.
     * @param def   The default value, if this argument is not given.
     * @return The argument's value, or {@code def} if none was provided.
     * @throws LuaException If the value is not a number.
     */
    public static int optInt( @Nonnull Object[] args, int index, int def ) throws LuaException
    {
        return (int) optLong( args, index, def );
    }

    /**
     * Get an argument as a long.
     *
     * @param args  The arguments to extract from.
     * @param index The index into the argument array to read from.
     * @param def   The default value, if this argument is not given.
     * @return The argument's value, or {@code def} if none was provided.
     * @throws LuaException If the value is not a number.
     */
    public static long optLong( @Nonnull Object[] args, int index, long def ) throws LuaException
    {
        Object value = index < args.length ? args[index] : null;
        if( value == null ) return def;
        if( !(value instanceof Number) ) throw badArgumentOf( index, "number", value );
        return checkFinite( index, (Number) value ).longValue();
    }

    /**
     * Get an argument as a finite number (not infinite or NaN).
     *
     * @param args  The arguments to extract from.
     * @param index The index into the argument array to read from.
     * @param def   The default value, if this argument is not given.
     * @return The argument's value, or {@code def} if none was provided.
     * @throws LuaException If the value is not finite.
     */
    public static double optFiniteDouble( @Nonnull Object[] args, int index, double def ) throws LuaException
    {
        return checkFinite( index, optDouble( args, index, def ) );
    }

    /**
     * Get an argument as a boolean.
     *
     * @param args  The arguments to extract from.
     * @param index The index into the argument array to read from.
     * @param def   The default value, if this argument is not given.
     * @return The argument's value, or {@code def} if none was provided.
     * @throws LuaException If the value is not a boolean.
     */
    public static boolean optBoolean( @Nonnull Object[] args, int index, boolean def ) throws LuaException
    {
        Object value = index < args.length ? args[index] : null;
        if( value == null ) return def;
        if( !(value instanceof Boolean) ) throw badArgumentOf( index, "boolean", value );
        return (Boolean) value;
    }

    /**
     * Get an argument as a string.
     *
     * @param args  The arguments to extract from.
     * @param index The index into the argument array to read from.
     * @param def   The default value, if this argument is not given.
     * @return The argument's value, or {@code def} if none was provided.
     * @throws LuaException If the value is not a string.
     */
    public static String optString( @Nonnull Object[] args, int index, String def ) throws LuaException
    {
        Object value = index < args.length ? args[index] : null;
        if( value == null ) return def;
        if( !(value instanceof String) ) throw badArgumentOf( index, "string", value );
        return (String) value;
    }

    /**
     * Get an argument as a table.
     *
     * @param args  The arguments to extract from.
     * @param index The index into the argument array to read from.
     * @param def   The default value, if this argument is not given.
     * @return The argument's value, or {@code def} if none was provided.
     * @throws LuaException If the value is not a table.
     */
    public static Map<?, ?> optTable( @Nonnull Object[] args, int index, Map<Object, Object> def ) throws LuaException
    {
        Object value = index < args.length ? args[index] : null;
        if( value == null ) return def;
        if( !(value instanceof Map) ) throw badArgumentOf( index, "table", value );
        return (Map<?, ?>) value;
    }

    private static Number checkFinite( int index, Number value ) throws LuaException
    {
        checkFinite( index, value.doubleValue() );
        return value;
    }

    private static double checkFinite( int index, double value ) throws LuaException
    {
        if( !Double.isFinite( value ) ) throw badArgument( index, "number", getNumericType( value ) );
        return value;
    }

    /**
     * Returns a more detailed representation of this number's type. If this is finite, it will just return "number",
     * otherwise it returns whether it is infinite or NaN.
     *
     * @param value The value to extract the type for.
     * @return This value's numeric type.
     */
    @Nonnull
    public static String getNumericType( double value )
    {
        if( Double.isNaN( value ) ) return "nan";
        if( value == Double.POSITIVE_INFINITY ) return "inf";
        if( value == Double.NEGATIVE_INFINITY ) return "-inf";
        return "number";
    }
}
