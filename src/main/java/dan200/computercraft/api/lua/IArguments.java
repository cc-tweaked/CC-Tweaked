/*
 * This file is part of the public ComputerCraft API - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. This API may be redistributed unmodified and in full only.
 * For help using the API, and posting your mods, visit the forums at computercraft.info.
 */
package dan200.computercraft.api.lua;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.Optional;

import static dan200.computercraft.api.lua.LuaValues.checkFinite;

/**
 * The arguments passed to a function.
 */
public interface IArguments
{
    /**
     * Get the number of arguments passed to this function.
     *
     * @return The number of passed arguments.
     */
    int count();

    /**
     * Get the argument at the specific index. The returned value must obey the following conversion rules:
     *
     * <ul>
     *   <li>Lua values of type "string" will be represented by a {@link String}.</li>
     *   <li>Lua values of type "number" will be represented by a {@link Number}.</li>
     *   <li>Lua values of type "boolean" will be represented by a {@link Boolean}.</li>
     *   <li>Lua values of type "table" will be represented by a {@link Map}.</li>
     *   <li>Lua values of any other type will be represented by a {@code null} value.</li>
     * </ul>
     *
     * @param index The argument number.
     * @return The argument's value, or {@code null} if not present.
     */
    @Nullable
    Object get( int index );

    /**
     * Drop a number of arguments. The returned arguments instance will access arguments at position {@code i + count},
     * rather than {@code i}. However, errors will still use the given argument index.
     *
     * @param count The number of arguments to drop.
     * @return The new {@link IArguments} instance.
     */
    IArguments drop( int count );

    default Object[] getAll()
    {
        Object[] result = new Object[count()];
        for( int i = 0; i < result.length; i++ ) result[i] = get( i );
        return result;
    }

    /**
     * Get an argument as a double.
     *
     * @param index The argument number.
     * @return The argument's value.
     * @throws LuaException If the value is not a number.
     * @see #getFiniteDouble(int) if you require this to be finite (i.e. not infinite or NaN).
     */
    default double getDouble( int index ) throws LuaException
    {
        Object value = get( index );
        if( !(value instanceof Number) ) throw LuaValues.badArgumentOf( index, "number", value );
        return ((Number) value).doubleValue();
    }

    /**
     * Get an argument as an integer.
     *
     * @param index The argument number.
     * @return The argument's value.
     * @throws LuaException If the value is not an integer.
     */
    default int getInt( int index ) throws LuaException
    {
        return (int) getLong( index );
    }

    /**
     * Get an argument as a long.
     *
     * @param index The argument number.
     * @return The argument's value.
     * @throws LuaException If the value is not a long.
     */
    default long getLong( int index ) throws LuaException
    {
        Object value = get( index );
        if( !(value instanceof Number) ) throw LuaValues.badArgumentOf( index, "number", value );
        return LuaValues.checkFiniteNum( index, (Number) value ).longValue();
    }

    /**
     * Get an argument as a finite number (not infinite or NaN).
     *
     * @param index The argument number.
     * @return The argument's value.
     * @throws LuaException If the value is not finite.
     */
    default double getFiniteDouble( int index ) throws LuaException
    {
        return checkFinite( index, getDouble( index ) );
    }

    /**
     * Get an argument as a boolean.
     *
     * @param index The argument number.
     * @return The argument's value.
     * @throws LuaException If the value is not a boolean.
     */
    default boolean getBoolean( int index ) throws LuaException
    {
        Object value = get( index );
        if( !(value instanceof Boolean) ) throw LuaValues.badArgumentOf( index, "boolean", value );
        return (Boolean) value;
    }

    /**
     * Get an argument as a string.
     *
     * @param index The argument number.
     * @return The argument's value.
     * @throws LuaException If the value is not a string.
     */
    @Nonnull
    default String getString( int index ) throws LuaException
    {
        Object value = get( index );
        if( !(value instanceof String) ) throw LuaValues.badArgumentOf( index, "string", value );
        return (String) value;
    }

    /**
     * Get a string argument as a byte array.
     *
     * @param index The argument number.
     * @return The argument's value. This is a <em>read only</em> buffer.
     * @throws LuaException If the value is not a string.
     */
    @Nonnull
    default ByteBuffer getBytes( int index ) throws LuaException
    {
        return LuaValues.encode( getString( index ) );
    }

    /**
     * Get a string argument as an enum value.
     *
     * @param index The argument number.
     * @param klass The type of enum to parse.
     * @param <T>   The type of enum to parse.
     * @return The argument's value.
     * @throws LuaException If the value is not a string or not a valid option for this enum.
     */
    @Nonnull
    default <T extends Enum<T>> T getEnum( int index, Class<T> klass ) throws LuaException
    {
        return LuaValues.checkEnum( index, klass, getString( index ) );
    }

    /**
     * Get an argument as a table.
     *
     * @param index The argument number.
     * @return The argument's value.
     * @throws LuaException If the value is not a table.
     */
    @Nonnull
    default Map<?, ?> getTable( int index ) throws LuaException
    {
        Object value = get( index );
        if( !(value instanceof Map) ) throw LuaValues.badArgumentOf( index, "table", value );
        return (Map<?, ?>) value;
    }

    /**
     * Get an argument as a table in an unsafe manner.
     *
     * Classes implementing this interface may choose to implement a more optimised version which does not copy the
     * table, instead returning a wrapper version, making it more efficient. However, the caller must guarantee that
     * they do not access off the computer thread (and so should not be used with main-thread functions) or once the
     * function call has finished (for instance, in callbacks).
     *
     * @param index The argument number.
     * @return The argument's value.
     * @throws LuaException If the value is not a table.
     */
    @Nonnull
    default LuaTable<?, ?> getTableUnsafe( int index ) throws LuaException
    {
        return new ObjectLuaTable( getTable( index ) );
    }

    /**
     * Get an argument as a double.
     *
     * @param index The argument number.
     * @return The argument's value, or {@link Optional#empty()} if not present.
     * @throws LuaException If the value is not a number.
     */
    @Nonnull
    default Optional<Double> optDouble( int index ) throws LuaException
    {
        Object value = get( index );
        if( value == null ) return Optional.empty();
        if( !(value instanceof Number) ) throw LuaValues.badArgumentOf( index, "number", value );
        return Optional.of( ((Number) value).doubleValue() );
    }

    /**
     * Get an argument as an int.
     *
     * @param index The argument number.
     * @return The argument's value, or {@link Optional#empty()} if not present.
     * @throws LuaException If the value is not a number.
     */
    @Nonnull
    default Optional<Integer> optInt( int index ) throws LuaException
    {
        return optLong( index ).map( Long::intValue );
    }

    /**
     * Get an argument as a long.
     *
     * @param index The argument number.
     * @return The argument's value, or {@link Optional#empty()} if not present.
     * @throws LuaException If the value is not a number.
     */
    default Optional<Long> optLong( int index ) throws LuaException
    {
        Object value = get( index );
        if( value == null ) return Optional.empty();
        if( !(value instanceof Number) ) throw LuaValues.badArgumentOf( index, "number", value );
        return Optional.of( LuaValues.checkFiniteNum( index, (Number) value ).longValue() );
    }

    /**
     * Get an argument as a finite number (not infinite or NaN).
     *
     * @param index The argument number.
     * @return The argument's value, or {@link Optional#empty()} if not present.
     * @throws LuaException If the value is not finite.
     */
    default Optional<Double> optFiniteDouble( int index ) throws LuaException
    {
        Optional<Double> value = optDouble( index );
        if( value.isPresent() ) LuaValues.checkFiniteNum( index, value.get() );
        return value;
    }

    /**
     * Get an argument as a boolean.
     *
     * @param index The argument number.
     * @return The argument's value, or {@link Optional#empty()} if not present.
     * @throws LuaException If the value is not a boolean.
     */
    default Optional<Boolean> optBoolean( int index ) throws LuaException
    {
        Object value = get( index );
        if( value == null ) return Optional.empty();
        if( !(value instanceof Boolean) ) throw LuaValues.badArgumentOf( index, "boolean", value );
        return Optional.of( (Boolean) value );
    }

    /**
     * Get an argument as a string.
     *
     * @param index The argument number.
     * @return The argument's value, or {@link Optional#empty()} if not present.
     * @throws LuaException If the value is not a string.
     */
    default Optional<String> optString( int index ) throws LuaException
    {
        Object value = get( index );
        if( value == null ) return Optional.empty();
        if( !(value instanceof String) ) throw LuaValues.badArgumentOf( index, "string", value );
        return Optional.of( (String) value );
    }

    /**
     * Get a string argument as a byte array.
     *
     * @param index The argument number.
     * @return The argument's value, or {@link Optional#empty()} if not present. This is a <em>read only</em> buffer.
     * @throws LuaException If the value is not a string.
     */
    default Optional<ByteBuffer> optBytes( int index ) throws LuaException
    {
        return optString( index ).map( LuaValues::encode );
    }

    /**
     * Get a string argument as an enum value.
     *
     * @param index The argument number.
     * @param klass The type of enum to parse.
     * @param <T>   The type of enum to parse.
     * @return The argument's value.
     * @throws LuaException If the value is not a string or not a valid option for this enum.
     */
    @Nonnull
    default <T extends Enum<T>> Optional<T> optEnum( int index, Class<T> klass ) throws LuaException
    {
        Optional<String> str = optString( index );
        return str.isPresent() ? Optional.of( LuaValues.checkEnum( index, klass, str.get() ) ) : Optional.empty();
    }

    /**
     * Get an argument as a table.
     *
     * @param index The argument number.
     * @return The argument's value, or {@link Optional#empty()} if not present.
     * @throws LuaException If the value is not a table.
     */
    default Optional<Map<?, ?>> optTable( int index ) throws LuaException
    {
        Object value = get( index );
        if( value == null ) return Optional.empty();
        if( !(value instanceof Map) ) throw LuaValues.badArgumentOf( index, "map", value );
        return Optional.of( (Map<?, ?>) value );
    }

    /**
     * Get an argument as a table in an unsafe manner.
     *
     * Classes implementing this interface may choose to implement a more optimised version which does not copy the
     * table, instead returning a wrapper version, making it more efficient. However, the caller must guarantee that
     * they do not access off the computer thread (and so should not be used with main-thread functions) or once the
     * function call has finished (for instance, in callbacks).
     *
     * @param index The argument number.
     * @return The argument's value, or {@link Optional#empty()} if not present.
     * @throws LuaException If the value is not a table.
     */
    @Nonnull
    default Optional<LuaTable<?, ?>> optTableUnsafe( int index ) throws LuaException
    {
        Object value = get( index );
        if( value == null ) return Optional.empty();
        if( !(value instanceof Map) ) throw LuaValues.badArgumentOf( index, "map", value );
        return Optional.of( new ObjectLuaTable( (Map<?, ?>) value ) );
    }

    /**
     * Get an argument as a double.
     *
     * @param index The argument number.
     * @param def   The default value, if this argument is not given.
     * @return The argument's value, or {@code def} if none was provided.
     * @throws LuaException If the value is not a number.
     */
    default double optDouble( int index, double def ) throws LuaException
    {
        return optDouble( index ).orElse( def );
    }

    /**
     * Get an argument as an int.
     *
     * @param index The argument number.
     * @param def   The default value, if this argument is not given.
     * @return The argument's value, or {@code def} if none was provided.
     * @throws LuaException If the value is not a number.
     */
    default int optInt( int index, int def ) throws LuaException
    {
        return optInt( index ).orElse( def );
    }

    /**
     * Get an argument as a long.
     *
     * @param index The argument number.
     * @param def   The default value, if this argument is not given.
     * @return The argument's value, or {@code def} if none was provided.
     * @throws LuaException If the value is not a number.
     */
    default long optLong( int index, long def ) throws LuaException
    {
        return optLong( index ).orElse( def );
    }

    /**
     * Get an argument as a finite number (not infinite or NaN).
     *
     * @param index The argument number.
     * @param def   The default value, if this argument is not given.
     * @return The argument's value, or {@code def} if none was provided.
     * @throws LuaException If the value is not finite.
     */
    default double optFiniteDouble( int index, double def ) throws LuaException
    {
        return optFiniteDouble( index ).orElse( def );
    }

    /**
     * Get an argument as a boolean.
     *
     * @param index The argument number.
     * @param def   The default value, if this argument is not given.
     * @return The argument's value, or {@code def} if none was provided.
     * @throws LuaException If the value is not a boolean.
     */
    default boolean optBoolean( int index, boolean def ) throws LuaException
    {
        return optBoolean( index ).orElse( def );
    }

    /**
     * Get an argument as a string.
     *
     * @param index The argument number.
     * @param def   The default value, if this argument is not given.
     * @return The argument's value, or {@code def} if none was provided.
     * @throws LuaException If the value is not a string.
     */
    default String optString( int index, String def ) throws LuaException
    {
        return optString( index ).orElse( def );
    }

    /**
     * Get an argument as a table.
     *
     * @param index The argument number.
     * @param def   The default value, if this argument is not given.
     * @return The argument's value, or {@code def} if none was provided.
     * @throws LuaException If the value is not a table.
     */
    default Map<?, ?> optTable( int index, Map<Object, Object> def ) throws LuaException
    {
        return optTable( index ).orElse( def );
    }

    /**
     * This is called when the current function finishes, before any main thread tasks have run.
     *
     * Called when the current function returns, and so some values are no longer guaranteed to be safe to access.
     */
    default void releaseImmediate()
    {
    }
}
