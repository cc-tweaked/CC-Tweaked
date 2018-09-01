/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2018. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.core.lua;

/**
 * A trivial way of signalling
 */
public final class Semaphore
{
    private volatile boolean state = false;

    public synchronized void signal()
    {
        state = true;
        notify();
    }

    public synchronized void await() throws InterruptedException
    {
        while( !state ) wait();
        state = false;
    }

    public synchronized boolean await( long timeout ) throws InterruptedException
    {
        if( !state )
        {
            wait( timeout );
            if( !state ) return false;
        }
        state = false;
        return true;
    }
}
