/*
 * This file is part of the public ComputerCraft API - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2018. This API may be redistributed unmodified and in full only.
 * For help using the API, and posting your mods, visit the forums at computercraft.info.
 */

package dan200.computercraft.api.lua;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.ListenableFuture;
import dan200.computercraft.api.peripheral.IComputerAccess;
import dan200.computercraft.api.peripheral.IPeripheral;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;

/**
 * The result of calling a method, such as {@link ILuaObject#callMethod(ICallContext, int, Object[])} or
 * {@link IPeripheral#callMethod(IComputerAccess, ICallContext, int, Object[])}.
 *
 * This is non-dissimilar to a promise or {@link ListenableFuture}. One can either return an immediate value through
 * {@link #of(Object...)}, wait for an external action with {@link #onMainThread(ILuaCallable)} or {@link #pullEvent()}
 * and then act on the result of either of those by using {@link #then(ILuaFunction)}.
 */
public abstract class MethodResult
{
    private static MethodResult empty;

    MethodResult()
    {
    }

    /**
     * A result which returns immediately with no value.
     *
     * Use {@link #of(Object...)} if you need to return one or more values.
     *
     * @return The empty method result.
     * @see #of(Object...)
     */
    @Nonnull
    public static MethodResult empty()
    {
        if( empty == null ) empty = new Immediate( null );
        return empty;
    }

    /**
     * A result which returns several values.
     *
     * @param result The values to return, this may be {@code null}. {@link Number}s, {@link String}s, {@link Boolean}s,
     *               {@link Map}s, {@link ILuaObject}s, and {@code null} be converted to their corresponding lua type.
     *               All other types will be converted to nil.
     * @return A result which will return these values when evaluated.
     * @see #empty()
     */
    @Nonnull
    public static MethodResult of( Object... result )
    {
        return result == null ? empty() : new Immediate( result );
    }

    /**
     * Wait for an event to occur on the computer, suspending the coroutine until it arises. This method is equivalent
     * to {@code os.pullEvent()} in Lua.
     *
     * Normally you'll wish to consume the event using {@link #then(ILuaFunction)}. This can be done slightly more
     * easily with {@link #pullEvent(ILuaFunction)}.
     *
     * If you want to listen to a specific event, it's easier to use {@link #pullEvent(String)} rather than
     * running until the desired event is found.
     *
     * @return The constructed method result. This evaluates to the name of the event that occurred, and any event
     * parameters.
     * @see #pullEvent(ILuaFunction)
     * @see #pullEvent(String)
     */
    @Nonnull
    public static MethodResult pullEvent()
    {
        return new OnEvent( false, null );
    }

    /**
     * Wait for the specified event to occur on the computer, suspending the coroutine until it arises. This method is
     * equivalent to {@code os.pullEvent(event)} in Lua.
     *
     * Normally you'll wish to consume the event using {@link #then(ILuaFunction)}. This can be done slightly more
     * easily with {@link #pullEvent(String, ILuaFunction)}.
     *
     * @return The constructed method result. This evaluates to the name of the event that occurred, and any event
     * parameters.
     * @see #pullEvent(String, ILuaFunction)
     * @see #pullEvent()
     */
    @Nonnull
    public static MethodResult pullEvent( @Nonnull String event )
    {
        Preconditions.checkNotNull( event, "event cannot be null" );
        return new OnEvent( false, event );
    }

    /**
     * Wait for an event to occur on the computer, suspending the coroutine until it arises. This method to
     * {@link #pullEvent()} and {@link #then(ILuaFunction)}.
     *
     * If you want to listen to a specific event, it's easier to use {@link #pullEvent(String, ILuaFunction)} rather
     * than running until the desired event is found.
     *
     * @return The constructed method result. This evaluates to the result of the {@code callback}.
     * @see #pullEvent()
     * @see #pullEvent(String, ILuaFunction)
     */
    @Nonnull
    public static MethodResult pullEvent( @Nonnull ILuaFunction callback )
    {
        Preconditions.checkNotNull( callback, "callback cannot be null" );
        return new OnEvent( false, null ).then( callback );
    }

    /**
     * Wait for the specified event to occur on the computer, suspending the coroutine until it arises. This method to
     * {@link #pullEvent(String)} and {@link #then(ILuaFunction)}.
     *
     * @return The constructed method result. This evaluates to the result of the {@code callback}.
     * @see #pullEvent(String)
     * @see #pullEvent(ILuaFunction)
     */
    @Nonnull
    public static MethodResult pullEvent( @Nullable String filter, @Nonnull ILuaFunction callback )
    {
        Preconditions.checkNotNull( callback, "callback cannot be null" );
        return new OnEvent( false, filter ).then( callback );
    }

    /**
     * The same as {@link #pullEvent()}, except {@code terminated} events are also passed to the callback, instead of
     * throwing an error. Only use this if you want to prevent program termination, which is not recommended.
     *
     * @return The constructed method result. This evaluates to the name of the event that occurred, and any event
     * parameters.
     */
    @Nonnull
    public static MethodResult pullEventRaw()
    {
        return new OnEvent( true, null );
    }

    /**
     * The same as {@link #pullEvent(String)}, except {@code terminated} events are also passed to the callback, instead
     * of throwing an error. Only use this if you want to prevent program termination, which is not recommended.
     *
     * @return The constructed method result. This evaluates to the name of the event that occurred, and any event
     * parameters.
     */
    @Nonnull
    public static MethodResult pullEventRaw( @Nonnull String event )
    {
        return new OnEvent( true, event );
    }

    /**
     * The same as {@link #pullEvent(ILuaFunction)}, except {@code terminated} events are also passed to the callback,
     * instead of throwing an error. Only use this if you want to prevent program termination, which is not recommended.
     *
     * @return The constructed method result. This evaluates to the result of the {@code callback}.
     */
    @Nonnull
    public static MethodResult pullEventRaw( @Nonnull ILuaFunction callback )
    {
        Preconditions.checkNotNull( callback, "callback cannot be null" );
        return new OnEvent( true, null ).then( callback );
    }

    /**
     * The same as {@link #pullEvent(String, ILuaFunction)}, except {@code terminated} events are also passed to the
     * callback, instead of throwing an error. Only use this if you want to prevent program termination, which is not
     * recommended.
     *
     * @return The constructed method result. This evaluates to the result of the {@code callback}.
     */
    @Nonnull
    public static MethodResult pullEventRaw( @Nullable String filter, @Nonnull ILuaFunction callback )
    {
        Preconditions.checkNotNull( callback, "callback cannot be null" );
        return new OnEvent( true, filter ).then( callback );
    }

    /**
     * Queue a task to be executed on the main server thread at the beginning of next tick, waiting for it to complete.
     * This should be used when you need to interact with the world in a thread-safe manner.
     *
     * @param callback The task to execute on the server thread.
     * @return The constructed method result, which evaluates to the result of the {@code callback}.
     */
    @Nonnull
    public static MethodResult onMainThread( @Nonnull ILuaCallable callback )
    {
        Preconditions.checkNotNull( callback, "callback cannot be null" );
        return new OnMainThread( callback );
    }

    /**
     * Consume the result of this {@link MethodResult} and return another result.
     *
     * Note this does NOT modify the current method result, rather returning a new (wrapped) one. You must return the
     * result of this call if you wish to use it.
     *
     * @param callback The function which consumes the provided values.
     * @return The constructed method result.
     */
    @Nonnull
    public final MethodResult then( @Nonnull ILuaFunction callback )
    {
        Preconditions.checkNotNull( callback, "callback cannot be null" );
        return new AndThen( this, callback );
    }

    /**
     * Execute a blocking task within a {@link ILuaContext} and return its result.
     *
     * @param consumer The task to execute with the provided Lua context.
     * @return The constructed method result.
     * @see #evaluate(ILuaContext)
     * @deprecated This should not be used except to interface between the two call systems.
     */
    @Deprecated
    public static MethodResult withLuaContext( @Nonnull ILuaContextTask consumer )
    {
        Preconditions.checkNotNull( consumer, "consumer cannot be null" );
        return new WithLuaContext( consumer );
    }

    /**
     * Evaluate this result task using {@link ILuaContext} and return its result.
     *
     * @param context The context to execute with.
     * @return The resulting values.
     * @see #withLuaContext(ILuaContextTask)
     * @deprecated This should not be used except to interface between the two call systems.
     */
    @Deprecated
    public final Object[] evaluate( @Nonnull ILuaContext context ) throws LuaException, InterruptedException
    {
        return LuaContextResultEvaluator.evaluate( context, this );
    }

    public static class Immediate extends MethodResult
    {
        @Nullable
        private final Object[] values;

        @Nullable
        private Immediate( Object[] values )
        {
            this.values = values;
        }

        public Object[] getResult()
        {
            return values;
        }
    }

    public static class OnEvent extends MethodResult
    {
        private final boolean raw;
        private final String filter;

        private OnEvent( boolean raw, String filter )
        {
            this.raw = raw;
            this.filter = filter;
        }

        public boolean isRaw()
        {
            return raw;
        }

        @Nullable
        public String getFilter()
        {
            return filter;
        }
    }

    public static class OnMainThread extends MethodResult
    {
        private final ILuaCallable task;

        public OnMainThread( ILuaCallable task )
        {
            this.task = task;
        }

        @Nonnull
        public ILuaCallable getTask()
        {
            return task;
        }
    }

    public static class AndThen extends MethodResult
    {
        private final MethodResult previous;
        private final ILuaFunction callback;

        private AndThen( MethodResult previous, ILuaFunction callback )
        {
            this.previous = previous;
            this.callback = callback;
        }

        @Nonnull
        public MethodResult getPrevious()
        {
            return previous;
        }

        @Nonnull
        public ILuaFunction getCallback()
        {
            return callback;
        }
    }

    public static class WithLuaContext extends MethodResult
    {
        private final ILuaContextTask consumer;

        private WithLuaContext( ILuaContextTask consumer )
        {
            this.consumer = consumer;
        }

        @Nonnull
        public ILuaContextTask getConsumer()
        {
            return consumer;
        }
    }
}
