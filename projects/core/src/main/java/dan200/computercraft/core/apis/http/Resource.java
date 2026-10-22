// SPDX-FileCopyrightText: 2019 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.core.apis.http;

import com.google.errorprone.annotations.CheckReturnValue;
import com.google.errorprone.annotations.MustBeClosed;
import dan200.computercraft.core.util.LockedReference;
import dan200.computercraft.core.util.ThreadUtils;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.*;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * A {@link Resource} is a value which does work outside the computer thread, but whose work can be stopped/interrupted
 * at any point by the computer shutting down. A resource may be updated by the computer (through Lua) or external
 * inputs (e.g. network code).
 * <p>
 * Due to the concurrent nature of resources (interruptions, Lua calls, and network calls may come at any point, and
 * sometimes be interleaved), a {@link Resource} explicitly models a state machine. Implementing classes provide a fixed
 * universe of states (represented by a sealed hierarchy of immutable {@link Resource.ResourceState}s), and transition
 * between those states with functions like {@link #transformState(Class, Function)} or
 * {@link #transformStateAsync(Class, BiFunction, Function)}.
 *
 * @param <R> The type of this resource. Should be the class extending from {@link Resource}.
 * @param <S> The internal state of this resource.
 */
public abstract class Resource<R extends Resource<R, S>, S extends Resource.ResourceState> {
    private static final Logger LOG = LoggerFactory.getLogger(Resource.class);
    final ResourceGroup<R> limiter;

    private final LockedReference.Mutable<@Nullable S> state;

    protected Resource(ResourceGroup<R> limiter, S state) {
        this.limiter = limiter;
        this.state = LockedReference.mutable("Resource.state", state);
    }

    /**
     * Start the work this resource owns.
     *
     * @see #queue()
     */
    @ApiStatus.OverrideOnly
    protected abstract void start();

    /**
     * Add this resource to the owning {@link ResourceGroup}.
     *
     * @return The group to add this resource to.
     */
    public final boolean queue() {
        @SuppressWarnings("unchecked")
        var thisT = (R) this;
        return limiter.queue(thisT);
    }

    /**
     * Whether this resource is closed.
     * <p>
     * This function is *not* thread safe, and so should only be used as a basic sanity check.
     *
     * @return Whether this resource is closed.
     */
    public final boolean isClosed() {
        try (var holder = state.lock()) {
            return holder.get() == null;
        }
    }

    /**
     * Get a read-only handle to the current state.
     *
     * @return A read-only handle to the current state.
     */
    @MustBeClosed
    protected final LockedReference.ReadHandle<@Nullable S> state() {
        return state.lock();
    }

    @CheckReturnValue
    private boolean setState(LockedReference.ReadWriteHandle<@Nullable S> stateHandle, @Nullable S newState) {
        var oldState = stateHandle.get();
        if (oldState == null) throw new IllegalStateException("Cannot transition from null state");

        stateHandle.set(newState);
        return newState == null;
    }

    @CheckReturnValue
    private boolean closeState(LockedReference.ReadWriteHandle<@Nullable S> stateHandle) {
        var currentState = stateHandle.get();
        if (currentState == null) return false;

        var closed = setState(stateHandle, null);
        currentState.close();
        return closed;
    }

    /**
     * Remove this resource from the limiter.
     * <p>
     * To avoid deadlocks, this function <em>MUST</em> be called when {@link #state} is not locked.
     */
    private void release() {
        @SuppressWarnings("unchecked")
        var self = (R) this;
        limiter.release(self);
    }

    final void close() {
        boolean closed;
        try (var handle = state.lock()) {
            closed = closeState(handle);
        }
        if (closed) release();
    }

    /**
     * Atomically transform the current state.
     * <p>
     * This function attempts to update the state with the supplied function. The function is passed the current state,
     * taking ownership of it (meaning it should consume or destroy any values that state owns). The resource's lock is
     * held for the duration of this function, so it should minimal work (e.g. constructing new instances, queuing
     * events), and should <strong>NOT</strong> block.
     * <p>
     * This function has no effect if the resource has already been closed.
     *
     * @param expectedState The state we expect this resource to be in.
     * @param transform     A function to update this state.
     * @param <E>           The type of expected state.
     */
    protected final <E extends S> void transformState(Class<E> expectedState, Function<E, @Nullable S> transform) {
        boolean closed;
        try (var stateHandle = state.lock()) {
            var state = stateHandle.get();
            if (state == null) return;
            if (!expectedState.isInstance(state)) {
                LOG.error("Unexpected transition: expected state to be a {}, but is {}. Closing", expectedState.getName(), state);
                closed = closeState(stateHandle);
            } else {
                closed = setState(stateHandle, transform.apply(expectedState.cast(state)));
            }
        }
        if (closed) release();
    }

    /**
     * Atomically transform the current state.
     * <p>
     * This function attempts to update the state with the supplied function. The function is passed the current state,
     * taking ownership of it (meaning it should consume or destroy any values that state owns). The resource's lock is
     * held for the duration of this function, so it should minimal work (e.g. constructing new instances, queuing
     * events), and should <strong>NOT</strong> block.
     * <p>
     * This function has no effect if the resource has already been closed.
     *
     * @param transform A function to update this state.
     */
    protected final void transformState(Function<S, @Nullable S> transform) {
        boolean closed;
        try (var stateHandle = state.lock()) {
            var state = stateHandle.get();
            if (state == null) return;
            closed = setState(stateHandle, transform.apply(state));
        }
        if (closed) release();
    }

    /**
     * Close the current resource, calling the supplied action if the resource is not already closed.
     *
     * @param expectedState The state we expect this resource to be in.
     * @param action        The action to run if this resource has not yet been called.
     */
    protected final void close(Class<? extends S> expectedState, Runnable action) {
        transformState(expectedState, s -> {
            action.run();
            s.close();
            return null;
        });
    }

    /**
     * Consume the current state and perform some long-running/blocking work on it, before transitioning into another
     * state.
     * <p>
     * While this work is running, the resource may be closed (cancelling the work), but should not transition to other
     * live states (e.g. via {@link #transformState(Function)}). Attempting to do so will logan error and immediately
     * close the resource.
     * <p>
     * Due to this, the work function should not return a new state directly. Instead, it returns an action that is
     * called in the result of a successful transition, similar to {@link #transformState(Function)}.
     *
     * @param expectedState The state we expect this resource to be in.
     * @param pendingState  A function to construct the "pending" state, which holds the previous state and this future.
     * @param work          The function that performs work on another thread.
     * @param <E>           The type of expected state.
     */
    protected final <E extends S> void transformStateAsync(
        Class<E> expectedState, BiFunction<E, Future<?>, S> pendingState, Function<E, StateTransition<S>> work
    ) {
        transformState(expectedState, oldState -> {
            var action = new Action<>(oldState, work);
            var task = Action.EXECUTOR.submit(action);
            return action.futureState = pendingState.apply(oldState, task);
        });
    }

    private final class Action<E extends S> implements Callable<Void> {
        private static final ExecutorService EXECUTOR;

        static {
            var executor = new ScheduledThreadPoolExecutor(4, ThreadUtils.lowPriorityFactory("Network"));
            executor.setKeepAliveTime(60, TimeUnit.SECONDS);
            EXECUTOR = executor;
        }

        private final E oldState;
        private final Function<E, StateTransition<S>> transform;
        private @Nullable S futureState;

        private Action(E oldState, Function<E, StateTransition<S>> transform) {
            this.oldState = oldState;
            this.transform = transform;
        }

        @Override
        public Void call() {
            var newState = transform.apply(oldState);
            boolean closed;
            try (var stateHandle = state.lock()) {
                var state = stateHandle.get();
                if (state == null) return null;
                if (state != futureState) {
                    LOG.error("Unexpected transition: expected state to be {}, but current state is {}. Closing.", futureState, state);
                    closed = closeState(stateHandle);
                } else {
                    closed = setState(stateHandle, newState.transition());
                }
            }

            if (closed) release();

            return null;
        }
    }

    /**
     * A function to perform a state transition in {@link #transformStateAsync(Class, BiFunction, Function)}.
     *
     * @param <S> The resulting state.
     */
    public interface StateTransition<S> {
        @Nullable S transition();

        /**
         * Close the current state machine, running some action if not already closed.
         *
         * @param action The action to run.
         * @param <S>    The type of state.
         * @return The {@link StateTransition} instance.
         * @see Resource#close(Class, Runnable)
         */
        static <S> StateTransition<S> close(Runnable action) {
            return () -> {
                action.run();
                return null;
            };
        }
    }

    /**
     * A base interface used to model a resource state. Implementations of {@link Resource} should create a sealed
     * sub-interface of this, then implement that on each state {@code record}.
     */
    public interface ResourceState extends AutoCloseable {
        @Override
        void close();
    }

    protected static void closeChannel(@Nullable ChannelFuture future) {
        if (future == null) return;
        closeFuture(future);
        closeChannel(future.channel());
    }

    protected static void closeChannel(@Nullable Channel channel) {
        if (channel != null && channel.isOpen()) channel.close();
    }

    protected static void closeFuture(@Nullable Future<?> future) {
        if (future != null) future.cancel(true);
    }
}
