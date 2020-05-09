/*
 * This file is part of the public ComputerCraft API - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. This API may be redistributed unmodified and in full only.
 * For help using the API, and posting your mods, visit the forums at computercraft.info.
 */
package dan200.computercraft.api.lua;

import dan200.computercraft.api.peripheral.IComputerAccess;
import dan200.computercraft.api.peripheral.IDynamicPeripheral;

import javax.annotation.Nonnull;
import java.util.Map;

/**
 * Provides methods for extracting values and validating Lua arguments, such as those provided to {@link LuaFunction}
 * or {@link IDynamicPeripheral#callMethod(IComputerAccess, ILuaContext, int, IArguments)}
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
        if( index >= args.length ) throw LuaValues.badArgument( index, "number", "nil" );
        Object value = args[index];
        if( !(value instanceof Number) ) throw LuaValues.badArgumentOf( index, "number", value );
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
        if( index >= args.length ) throw LuaValues.badArgument( index, "number", "nil" );
        Object value = args[index];
        if( !(value instanceof Number) ) throw LuaValues.badArgumentOf( index, "number", value );
        return LuaValues.checkFinite( index, (Number) value ).longValue();
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
        return LuaValues.checkFinite( index, getDouble( args, index ) );
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
        if( index >= args.length ) throw LuaValues.badArgument( index, "boolean", "nil" );
        Object value = args[index];
        if( !(value instanceof Boolean) ) throw LuaValues.badArgumentOf( index, "boolean", value );
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
        if( index >= args.length ) throw LuaValues.badArgument( index, "string", "nil" );
        Object value = args[index];
        if( !(value instanceof String) ) throw LuaValues.badArgumentOf( index, "string", value );
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
        if( index >= args.length ) throw LuaValues.badArgument( index, "table", "nil" );
        Object value = args[index];
        if( !(value instanceof Map) ) throw LuaValues.badArgumentOf( index, "table", value );
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
        if( !(value instanceof Number) ) throw LuaValues.badArgumentOf( index, "number", value );
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
        if( !(value instanceof Number) ) throw LuaValues.badArgumentOf( index, "number", value );
        return LuaValues.checkFinite( index, (Number) value ).longValue();
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
        return LuaValues.checkFinite( index, optDouble( args, index, def ) );
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
        if( !(value instanceof Boolean) ) throw LuaValues.badArgumentOf( index, "boolean", value );
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
        if( !(value instanceof String) ) throw LuaValues.badArgumentOf( index, "string", value );
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
        if( !(value instanceof Map) ) throw LuaValues.badArgumentOf( index, "table", value );
        return (Map<?, ?>) value;
    }

}
