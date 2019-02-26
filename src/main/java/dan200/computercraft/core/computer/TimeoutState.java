/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.core.computer;

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
     * The total time a task is allowed to run before aborting in milliseconds
     */
    static final long TIMEOUT = 7000;

    /**
     * The time the task is allowed to run after each abort in milliseconds
     */
    static final long ABORT_TIMEOUT = 1500;

    public static final String ABORT_MESSAGE = "Too long without yielding";

    private volatile boolean softAbort;
    private volatile boolean hardAbort;

    private long milliTime;
    private long nanoTime;

    long milliSinceStart()
    {
        return System.currentTimeMillis() - milliTime;
    }

    long nanoSinceStart()
    {
        return System.nanoTime() - nanoTime;
    }

    /**
     * If the machine should be passively aborted.
     */
    public boolean isSoftAborted()
    {
        return softAbort || (softAbort = (System.currentTimeMillis() - milliTime) >= TIMEOUT);
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
     * Reset all abort flags and start the abort timer.
     */
    void reset()
    {
        softAbort = hardAbort = false;
        milliTime = System.currentTimeMillis();
        nanoTime = System.nanoTime();
    }
}
