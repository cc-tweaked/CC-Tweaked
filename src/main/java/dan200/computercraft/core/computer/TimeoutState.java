/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.core.computer;

import dan200.computercraft.core.lua.ILuaMachine;
import dan200.computercraft.core.lua.MachineResult;

import java.util.concurrent.TimeUnit;

/**
 * Used to measure how long a computer has executed for, and thus the relevant timeout states.
 *
 * Timeouts are mostly used for execution of Lua code: we should ideally never have a state where constructing the APIs
 * or machines themselves takes more than a fraction of a second.
 *
 * When a computer runs, it is allowed to run for 7 seconds ({@link #TIMEOUT}). After that point, the "soft abort" flag
 * is set ({@link #isSoftAborted()}). Here, the Lua machine will attempt to abort the program in some safe manner
 * (namely, throwing a "Too long without yielding" error).
 *
 * Now, if a computer still does not stop after that period, they're behaving really badly. 1.5 seconds after a soft
 * abort ({@link #ABORT_TIMEOUT}), we trigger a hard abort (note, this is done from the computer thread manager). This
 * will destroy the entire Lua runtime and shut the computer down.
 *
 * The Lua runtime is also allowed to pause execution if there are other computers contesting for work. All computers
 * are allowed to run for {@link ComputerThread#scaledPeriod()} nanoseconds (see {@link #currentDeadline}). After that
 * period, if any computers are waiting to be executed then we'll set the paused flag to true ({@link #isPaused()}.
 *
 * @see ComputerThread
 * @see ILuaMachine
 * @see MachineResult#isPause()
 */
public final class TimeoutState
{
    /**
     * The total time a task is allowed to run before aborting in nanoseconds.
     */
    static final long TIMEOUT = TimeUnit.MILLISECONDS.toNanos( 7000 );

    /**
     * The time the task is allowed to run after each abort in nanoseconds.
     */
    static final long ABORT_TIMEOUT = TimeUnit.MILLISECONDS.toNanos( 1500 );

    /**
     * The error message to display when we trigger an abort.
     */
    public static final String ABORT_MESSAGE = "Too long without yielding";

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

    long nanoCumulative()
    {
        return System.nanoTime() - cumulativeStart;
    }

    long nanoCurrent()
    {
        return System.nanoTime() - currentStart;
    }

    /**
     * Recompute the {@link #isSoftAborted()} and {@link #isPaused()} flags.
     */
    public void refresh()
    {
        // Important: The weird arithmetic here is important, as nanoTime may return negative values, and so we
        // need to handle overflow.
        long now = System.nanoTime();
        if( !paused ) paused = currentDeadline - now <= 0 && ComputerThread.hasPendingWork(); // now >= currentDeadline
        if( !softAbort ) softAbort = now - cumulativeStart - TIMEOUT >= 0; // now - cumulativeStart >= TIMEOUT
    }

    /**
     * Whether we should pause execution of this machine.
     *
     * This is determined by whether we've consumed our time slice, and if there are other computers waiting to perform
     * work.
     *
     * @return Whether we should pause execution.
     */
    public boolean isPaused()
    {
        return paused;
    }

    /**
     * If the machine should be passively aborted.
     *
     * @return {@code true} if we should throw a timeout error.
     */
    public boolean isSoftAborted()
    {
        return softAbort;
    }

    /**
     * Determine if the machine should be forcibly aborted.
     *
     * @return {@code true} if the machine should be forcibly shut down.
     */
    public boolean isHardAborted()
    {
        return hardAbort;
    }

    /**
     * If the machine should be forcibly aborted.
     */
    void hardAbort()
    {
        softAbort = hardAbort = true;
    }

    /**
     * Start the current and cumulative timers again.
     */
    void startTimer()
    {
        long now = System.nanoTime();
        currentStart = now;
        currentDeadline = now + ComputerThread.scaledPeriod();
        // Compute the "nominal start time".
        cumulativeStart = now - cumulativeElapsed;
    }

    /**
     * Pauses the cumulative time, to be resumed by {@link #startTimer()}.
     *
     * @see #nanoCumulative()
     */
    void pauseTimer()
    {
        // We set the cumulative time to difference between current time and "nominal start time".
        cumulativeElapsed = System.nanoTime() - cumulativeStart;
        paused = false;
    }

    /**
     * Resets the cumulative time and resets the abort flags.
     */
    void stopTimer()
    {
        cumulativeElapsed = 0;
        paused = softAbort = hardAbort = false;
    }
}
