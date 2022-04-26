/*
 * This file is part of the public ComputerCraft API - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. This API may be redistributed unmodified and in full only.
 * For help using the API, and posting your mods, visit the forums at computercraft.info.
 */
package dan200.computercraft.api.lua;

import dan200.computercraft.api.peripheral.IComputerAccess;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;

/**
 * The result of invoking a Lua method.
 *
 * Method results either return a value immediately ({@link #of(Object...)} or yield control to the parent coroutine.
 * When the current coroutine is resumed, we invoke the provided {@link ILuaCallback#resume(Object[])} callback.
 */
public final class MethodResult
{
    private static final MethodResult empty = new MethodResult( null, null );

    private final Object[] result;
    private final ILuaCallback callback;
    private final int adjust;

    private MethodResult( Object[] arguments, ILuaCallback callback )
    {
        result = arguments;
        this.callback = callback;
        adjust = 0;
    }

    private MethodResult( Object[] arguments, ILuaCallback callback, int adjust )
    {
        result = arguments;
        this.callback = callback;
        this.adjust = adjust;
    }

    /**
     * Return no values immediately.
     *
     * @return A method result which returns immediately with no values.
     */
    @Nonnull
    public static MethodResult of()
    {
        return empty;
    }

    /**
     * Return a single value immediately.
     *
     * Integers, doubles, floats, strings, booleans, {@link Map}, {@link Collection}s, arrays and {@code null} will be
     * converted to their corresponding Lua type. {@code byte[]} and {@link ByteBuffer} will be treated as binary
     * strings. {@link ILuaFunction} will be treated as a function.
     *
     * In order to provide a custom object with methods, one may return a {@link IDynamicLuaObject}, or an arbitrary
     * class with {@link LuaFunction} annotations. Anything else will be converted to {@code nil}.
     *
     * @param value The value to return to the calling Lua function.
     * @return A method result which returns immediately with the given value.
     */
    @Nonnull
    public static MethodResult of( @Nullable Object value )
    {
        return new MethodResult( new Object[] { value }, null );
    }

    /**
     * Return any number of values immediately.
     *
     * @param values The values to return. See {@link #of(Object)} for acceptable values.
     * @return A method result which returns immediately with the given values.
     */
    @Nonnull
    public static MethodResult of( @Nullable Object... values )
    {
        return values == null || values.length == 0 ? empty : new MethodResult( values, null );
    }

    /**
     * Wait for an event to occur on the computer, suspending the thread until it arises. This method is exactly
     * equivalent to {@code os.pullEvent()} in lua.
     *
     * @param filter   A specific event to wait for, or null to wait for any event.
     * @param callback The callback to resume with the name of the event that occurred, and any event parameters.
     * @return The method result which represents this yield.
     * @see IComputerAccess#queueEvent(String, Object[])
     */
    @Nonnull
    public static MethodResult pullEvent( @Nullable String filter, @Nonnull ILuaCallback callback )
    {
        Objects.requireNonNull( callback, "callback cannot be null" );
        return new MethodResult( new Object[] { filter }, results -> {
            if( results.length >= 1 && Objects.equals( results[0], "terminate" ) )
            {
                throw new LuaException( "Terminated", 0 );
            }
            return callback.resume( results );
        } );
    }

    /**
     * The same as {@link #pullEvent(String, ILuaCallback)}, except "terminated" events are ignored. Only use this if
     * you want to prevent program termination, which is not recommended. This method is exactly equivalent to
     * {@code os.pullEventRaw()} in Lua.
     *
     * @param filter   A specific event to wait for, or null to wait for any event.
     * @param callback The callback to resume with the name of the event that occurred, and any event parameters.
     * @return The method result which represents this yield.
     * @see #pullEvent(String, ILuaCallback)
     */
    @Nonnull
    public static MethodResult pullEventRaw( @Nullable String filter, @Nonnull ILuaCallback callback )
    {
        Objects.requireNonNull( callback, "callback cannot be null" );
        return new MethodResult( new Object[] { filter }, callback );
    }

    /**
     * Yield the current coroutine with some arguments until it is resumed. This method is exactly equivalent to
     * {@code coroutine.yield()} in lua. Use {@code pullEvent()} if you wish to wait for events.
     *
     * @param arguments An object array containing the arguments to pass to coroutine.yield()
     * @param callback  The callback to resume with an array containing the return values from coroutine.yield()
     * @return The method result which represents this yield.
     * @see #pullEvent(String, ILuaCallback)
     */
    @Nonnull
    public static MethodResult yield( @Nullable Object[] arguments, @Nonnull ILuaCallback callback )
    {
        Objects.requireNonNull( callback, "callback cannot be null" );
        return new MethodResult( arguments, callback );
    }

    @Nullable
    public Object[] getResult()
    {
        return result;
    }

    @Nullable
    public ILuaCallback getCallback()
    {
        return callback;
    }

    public int getErrorAdjust()
    {
        return adjust;
    }

    /**
     * Increase the Lua error by a specific amount. One should never need to use this function - it largely exists for
     * some CC internal code.
     *
     * @param adjust The amount to increase the level by.
     * @return The new {@link MethodResult} with an adjusted error. This has no effect on immediate results.
     */
    @Nonnull
    public MethodResult adjustError( int adjust )
    {
        if( adjust < 0 ) throw new IllegalArgumentException( "cannot adjust by a negative amount" );
        if( adjust == 0 || callback == null ) return this;
        return new MethodResult( result, callback, this.adjust + adjust );
    }
}
