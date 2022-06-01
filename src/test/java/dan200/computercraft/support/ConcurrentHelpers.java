/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.support;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;
import java.util.function.BooleanSupplier;

/**
 * Utilities for working with concurrent systems.
 */
public class ConcurrentHelpers
{
    private static final long DELAY = TimeUnit.MILLISECONDS.toNanos( 2 );

    /**
     * Wait until a condition is true, checking the condition every 2ms.
     *
     * @param isTrue The condition to check
     * @return How long we waited for.
     */
    public static long waitUntil( BooleanSupplier isTrue )
    {
        long start = System.nanoTime();
        while( true )
        {
            if( isTrue.getAsBoolean() ) return System.nanoTime() - start;
            LockSupport.parkNanos( DELAY );
        }
    }

    /**
     * Wait until a condition is true or a timeout is elapsed, checking the condition every 2ms.
     *
     * @param isTrue  The condition to check
     * @param timeout The delay after which we will timeout.
     * @param unit    The time unit the duration is measured in.
     * @return {@literal true} if the condition was met, {@literal false} if we timed out instead.
     */
    public static boolean waitUntil( BooleanSupplier isTrue, long timeout, TimeUnit unit )
    {
        long start = System.nanoTime();
        long timeoutNs = unit.toNanos( timeout );
        while( true )
        {
            long time = System.nanoTime() - start;
            if( isTrue.getAsBoolean() ) return true;
            if( time > timeoutNs ) return false;

            LockSupport.parkNanos( DELAY );
        }
    }
}
