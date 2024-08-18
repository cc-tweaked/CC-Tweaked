// SPDX-FileCopyrightText: 2020 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.api.lua;

import dan200.computercraft.api.peripheral.IComputerAccess;

import javax.annotation.Nullable;
import java.nio.ByteBuffer;
import java.util.*;

/**
 * The result of invoking a Lua method.
 * <p>
 * Method results either return a value immediately ({@link #of(Object...)} or yield control to the parent coroutine.
 * When the current coroutine is resumed, we invoke the provided {@link ILuaCallback#resume(Object[])} callback.
 */
public final class MethodResult {
    private static final MethodResult empty = new MethodResult(null, null);

    private final @Nullable Object[] result;
    private final @Nullable ILuaCallback callback;
    private final int adjust;

    private MethodResult(@Nullable Object[] arguments, @Nullable ILuaCallback callback) {
        result = arguments;
        this.callback = callback;
        adjust = 0;
    }

    private MethodResult(@Nullable Object[] arguments, @Nullable ILuaCallback callback, int adjust) {
        result = arguments;
        this.callback = callback;
        this.adjust = adjust;
    }

    /**
     * Return no values immediately.
     *
     * @return A method result which returns immediately with no values.
     */
    public static MethodResult of() {
        return empty;
    }

    /**
     * Return a single value immediately.
     * <p>
     * Integers, doubles, floats, strings, booleans, {@link Map}, {@link Collection}s, arrays and {@code null} will be
     * converted to their corresponding Lua type. {@code byte[]} and {@link ByteBuffer} will be treated as binary
     * strings. {@link ILuaFunction} will be treated as a function.
     * <p>
     * In order to provide a custom object with methods, one may return a {@link IDynamicLuaObject}, or an arbitrary
     * class with {@link LuaFunction} annotations. Anything else will be converted to {@code nil}.
     * <p>
     * Shared objects in a {@link MethodResult} will preserve their sharing when converted to Lua values. For instance,
     * {@code Map<?, ?> m = new HashMap(); return MethodResult.of(m, m); } will return two values {@code a}, {@code b}
     * where {@code a == b}. The one exception to this is Java's singleton collections ({@link List#of()},
     * {@link Set#of()} and {@link Map#of()}), which are always converted to new table. This is not true for other
     * singleton collections, such as those provided by {@link Collections} or Guava.
     *
     * @param value The value to return to the calling Lua function.
     * @return A method result which returns immediately with the given value.
     */
    public static MethodResult of(@Nullable Object value) {
        return new MethodResult(new Object[]{ value }, null);
    }

    /**
     * Return any number of values immediately.
     *
     * @param values The values to return. See {@link #of(Object)} for acceptable values.
     * @return A method result which returns immediately with the given values.
     */
    public static MethodResult of(@Nullable Object... values) {
        return values == null || values.length == 0 ? empty : new MethodResult(values, null);
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
    public static MethodResult pullEvent(@Nullable String filter, ILuaCallback callback) {
        Objects.requireNonNull(callback, "callback cannot be null");
        return new MethodResult(new Object[]{ filter }, results -> {
            if (results.length >= 1 && Objects.equals(results[0], "terminate")) {
                throw new LuaException("Terminated", 0);
            }
            return callback.resume(results);
        });
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
    public static MethodResult pullEventRaw(@Nullable String filter, ILuaCallback callback) {
        Objects.requireNonNull(callback, "callback cannot be null");
        return new MethodResult(new Object[]{ filter }, callback);
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
    @SuppressWarnings("NamedLikeContextualKeyword")
    public static MethodResult yield(@Nullable Object[] arguments, ILuaCallback callback) {
        Objects.requireNonNull(callback, "callback cannot be null");
        return new MethodResult(arguments, callback);
    }

    @Nullable
    public Object[] getResult() {
        return result;
    }

    @Nullable
    public ILuaCallback getCallback() {
        return callback;
    }

    public int getErrorAdjust() {
        return adjust;
    }

    /**
     * Increase the Lua error by a specific amount. One should never need to use this function - it largely exists for
     * some CC internal code.
     *
     * @param adjust The amount to increase the level by.
     * @return The new {@link MethodResult} with an adjusted error. This has no effect on immediate results.
     */
    public MethodResult adjustError(int adjust) {
        if (adjust < 0) throw new IllegalArgumentException("cannot adjust by a negative amount");
        if (adjust == 0 || callback == null) return this;
        return new MethodResult(result, callback, this.adjust + adjust);
    }
}
