/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.core.computer;

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
 * @see ComputerThread
 * @see dan200.computercraft.core.lua.ILuaMachine
 */
public final class TimeoutState
{
    /**
     * The time to run a task before pausing in nanoseconds
     */
    private static final long TIMESLICE = TimeUnit.MILLISECONDS.toNanos( 40 );

    /**
     * The total time a task is allowed to run before aborting in nanoseconds
     */
    static final long TIMEOUT = TimeUnit.MILLISECONDS.toNanos( 7000 );

    /**
     * The time the task is allowed to run after each abort in nanoseconds
     */
    static final long ABORT_TIMEOUT = TimeUnit.MILLISECONDS.toNanos( 1500 );

    /**
     * The error message to display when we trigger an abort.
     */
    public static final String ABORT_MESSAGE = "Too long without yielding";

    private boolean paused;
    private boolean softAbort;
    private volatile boolean hardAbort;

    private long nanoCumulative;
    private long nanoCurrent;

    long nanoCumulative()
    {
        return System.nanoTime() - nanoCumulative;
    }

    long nanoCurrent()
    {
        return System.nanoTime() - nanoCurrent;
    }

    /**
     * Recompute the {@link #isSoftAborted()} and {@link #isPaused()} flags.
     */
    public void refresh()
    {
        long now = System.nanoTime();
        if( !paused ) paused = (now - nanoCurrent) >= TIMESLICE;
        if( !softAbort ) softAbort = (now - nanoCumulative) >= TIMEOUT;
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
        return paused && ComputerThread.hasPendingWork();
    }

    /**
     * If the machine should be passively aborted.
     */
    public boolean isSoftAborted()
    {
        return softAbort;
    }

    /**
     * If the machine should be forcibly aborted.
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
        nanoCurrent = now;
        // Compute the "nominal start time".
        nanoCumulative = now - nanoCumulative;
    }

    /**
     * Pauses the cumulative time, to be resumed by {@link #startTimer()}
     *
     * @see #nanoCumulative()
     */
    void pauseTimer()
    {
        // We set the cumulative time to difference between current time and "nominal start time".
        nanoCumulative = System.nanoTime() - nanoCumulative;
        paused = false;
    }

    /**
     * Resets the cumulative time and resets the abort flags.
     */
    void stopTimer()
    {
        nanoCumulative = 0;
        paused = softAbort = hardAbort = false;
    }
}
