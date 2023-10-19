// SPDX-FileCopyrightText: 2019 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.core.computer;

import com.google.errorprone.annotations.concurrent.GuardedBy;
import dan200.computercraft.core.computer.computerthread.ComputerScheduler;
import dan200.computercraft.core.computer.computerthread.ManagedTimeoutState;
import dan200.computercraft.core.lua.ILuaMachine;
import dan200.computercraft.core.lua.MachineResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * Used to measure how long a computer has executed for, and thus the relevant timeout states.
 * <p>
 * Timeouts are mostly used for execution of Lua code: we should ideally never have a state where constructing the APIs
 * or machines themselves takes more than a fraction of a second.
 * <p>
 * When a computer runs, it is allowed to run for 7 seconds ({@link #TIMEOUT}). After that point, the "soft abort" flag
 * is set ({@link #isSoftAborted()}). Here, the Lua machine will attempt to abort the program in some safe manner
 * (namely, throwing a "Too long without yielding" error).
 * <p>
 * Now, if a computer still does not stop after that period, they're behaving really badly. 1.5 seconds after a soft
 * abort ({@link #ABORT_TIMEOUT}), we trigger a hard abort. This will destroy the entire Lua runtime and shut the
 * computer down.
 * <p>
 * The Lua runtime is also allowed to pause execution if there are other computers contesting for work. All computers
 * are guaranteed to run for some time. After that period, if any computers are waiting to be executed then we'll set
 * the paused flag to true ({@link #isPaused()}.
 *
 * @see ComputerScheduler
 * @see ManagedTimeoutState
 * @see ILuaMachine
 * @see MachineResult#isPause()
 */
public abstract class TimeoutState {
    /**
     * The time (in nanoseconds) are computer is allowed to run for its long-running tasks, such as startup and
     * shutdown.
     */
    public static final long BASE_TIMEOUT = TimeUnit.SECONDS.toNanos(30);

    /**
     * The total time the Lua VM is allowed to run before aborting in nanoseconds.
     */
    public static final long TIMEOUT = TimeUnit.MILLISECONDS.toNanos(7000);

    /**
     * The time the task is allowed to run after each abort in nanoseconds.
     */
    public static final long ABORT_TIMEOUT = TimeUnit.MILLISECONDS.toNanos(1500);

    /**
     * The error message to display when we trigger an abort.
     */
    public static final String ABORT_MESSAGE = "Too long without yielding";

    @GuardedBy("this")
    private final List<Runnable> listeners = new ArrayList<>(0);

    protected boolean paused;
    protected boolean softAbort;
    protected volatile boolean hardAbort;

    /**
     * Recompute the {@link #isSoftAborted()} and {@link #isPaused()} flags.
     * <p>
     * Normally this will be called automatically by the {@link ComputerScheduler}, but it may be useful to call this
     * manually if the most up-to-date information is needed.
     */
    public abstract void refresh();

    /**
     * Whether we should pause execution of this machine.
     * <p>
     * This is determined by whether we've consumed our time slice, and if there are other computers waiting to perform
     * work.
     *
     * @return Whether we should pause execution.
     */
    public final boolean isPaused() {
        return paused;
    }

    /**
     * If the machine should be passively aborted.
     *
     * @return {@code true} if we should throw a timeout error.
     */
    public final boolean isSoftAborted() {
        return softAbort;
    }

    /**
     * Determine if the machine should be forcibly aborted.
     *
     * @return {@code true} if the machine should be forcibly shut down.
     */
    public final boolean isHardAborted() {
        return hardAbort;
    }

    @GuardedBy("this")
    protected final void updateListeners() {
        for (var listener : listeners) listener.run();
    }

    public final synchronized void addListener(Runnable listener) {
        Objects.requireNonNull(listener, "listener cannot be null");
        listeners.add(listener);
        listener.run();
    }

    public final synchronized void removeListener(Runnable listener) {
        Objects.requireNonNull(listener, "listener cannot be null");
        listeners.remove(listener);
    }
}
