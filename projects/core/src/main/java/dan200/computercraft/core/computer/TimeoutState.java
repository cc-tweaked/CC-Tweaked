// SPDX-FileCopyrightText: 2019 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.core.computer;

import com.google.errorprone.annotations.concurrent.GuardedBy;
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
 * abort ({@link #ABORT_TIMEOUT}), we trigger a hard abort (note, this is done from the computer thread manager). This
 * will destroy the entire Lua runtime and shut the computer down.
 * <p>
 * The Lua runtime is also allowed to pause execution if there are other computers contesting for work. All computers
 * are allowed to run for {@link ComputerThread#scaledPeriod()} nanoseconds (see {@link #currentDeadline}). After that
 * period, if any computers are waiting to be executed then we'll set the paused flag to true ({@link #isPaused()}.
 *
 * @see ComputerThread
 * @see ILuaMachine
 * @see MachineResult#isPause()
 */
public final class TimeoutState {
    /**
     * The total time a task is allowed to run before aborting in nanoseconds.
     */
    static final long TIMEOUT = TimeUnit.MILLISECONDS.toNanos(7000);

    /**
     * The time the task is allowed to run after each abort in nanoseconds.
     */
    static final long ABORT_TIMEOUT = TimeUnit.MILLISECONDS.toNanos(1500);

    /**
     * The error message to display when we trigger an abort.
     */
    public static final String ABORT_MESSAGE = "Too long without yielding";

    private final ComputerThread scheduler;
    @GuardedBy("this")
    private final List<Runnable> listeners = new ArrayList<>(0);

    private boolean paused;
    private boolean softAbort;
    private volatile boolean hardAbort;

    /**
     * When the cumulative time would have started had the whole event been processed in one go.
     */
    private long cumulativeStart;

    /**
     * How much cumulative time has elapsed. This is effectively {@code cumulativeStart - currentStart}.
     */
    private long cumulativeElapsed;

    /**
     * When this execution round started.
     */
    private long currentStart;

    /**
     * When this execution round should look potentially be paused.
     */
    private long currentDeadline;

    public TimeoutState(ComputerThread scheduler) {
        this.scheduler = scheduler;
    }

    long nanoCumulative() {
        return System.nanoTime() - cumulativeStart;
    }

    long nanoCurrent() {
        return System.nanoTime() - currentStart;
    }

    /**
     * Recompute the {@link #isSoftAborted()} and {@link #isPaused()} flags.
     */
    public synchronized void refresh() {
        // Important: The weird arithmetic here is important, as nanoTime may return negative values, and so we
        // need to handle overflow.
        var now = System.nanoTime();
        var changed = false;
        if (!paused && (paused = currentDeadline - now <= 0 && scheduler.hasPendingWork())) { // now >= currentDeadline
            changed = true;
        }
        if (!softAbort && (softAbort = now - cumulativeStart - TIMEOUT >= 0)) { // now - cumulativeStart >= TIMEOUT
            changed = true;
        }

        if (changed) updateListeners();
    }

    /**
     * Whether we should pause execution of this machine.
     * <p>
     * This is determined by whether we've consumed our time slice, and if there are other computers waiting to perform
     * work.
     *
     * @return Whether we should pause execution.
     */
    public boolean isPaused() {
        return paused;
    }

    /**
     * If the machine should be passively aborted.
     *
     * @return {@code true} if we should throw a timeout error.
     */
    public boolean isSoftAborted() {
        return softAbort;
    }

    /**
     * Determine if the machine should be forcibly aborted.
     *
     * @return {@code true} if the machine should be forcibly shut down.
     */
    public boolean isHardAborted() {
        return hardAbort;
    }

    /**
     * If the machine should be forcibly aborted.
     */
    void hardAbort() {
        softAbort = hardAbort = true;
        synchronized (this) {
            updateListeners();
        }
    }

    /**
     * Start the current and cumulative timers again.
     */
    void startTimer() {
        var now = System.nanoTime();
        currentStart = now;
        currentDeadline = now + scheduler.scaledPeriod();
        // Compute the "nominal start time".
        cumulativeStart = now - cumulativeElapsed;
    }

    /**
     * Pauses the cumulative time, to be resumed by {@link #startTimer()}.
     *
     * @see #nanoCumulative()
     */
    synchronized void pauseTimer() {
        // We set the cumulative time to difference between current time and "nominal start time".
        cumulativeElapsed = System.nanoTime() - cumulativeStart;
        paused = false;
        updateListeners();
    }

    /**
     * Resets the cumulative time and resets the abort flags.
     */
    synchronized void stopTimer() {
        cumulativeElapsed = 0;
        paused = softAbort = hardAbort = false;
        updateListeners();
    }

    @GuardedBy("this")
    private void updateListeners() {
        for (var listener : listeners) listener.run();
    }

    public synchronized void addListener(Runnable listener) {
        Objects.requireNonNull(listener, "listener cannot be null");
        listeners.add(listener);
        listener.run();
    }

    public synchronized void removeListener(Runnable listener) {
        Objects.requireNonNull(listener, "listener cannot be null");
        listeners.remove(listener);
    }
}
