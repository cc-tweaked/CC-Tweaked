/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.core.computer;

/**
 * Represents what flags have currently been set on the computer timeout
 *
 * @see ComputerThread
 * @see dan200.computercraft.core.lua.ILuaMachine
 */
public final class TimeoutFlags
{
    public static final String ABORT_MESSAGE = "Too long without yielding";

    private volatile boolean softAbort;
    private volatile boolean hardAbort;

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

    void softAbort()
    {
        softAbort = true;
    }

    /**
     * If the machine should be forcibly aborted.
     */
    void hardAbort()
    {
        // TODO: Queue a shutdown or something - if we don't halt when hard aborting, we'll never actually shut
        //  the thing down.
        softAbort = hardAbort = true;
    }

    /**
     * Reset all other flags
     */
    void resetAbort()
    {
        softAbort = hardAbort = false;
    }
}
