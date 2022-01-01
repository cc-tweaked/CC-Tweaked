/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class CallCounter implements Runnable
{
    private int timesCalled = 0;

    @Override
    public void run()
    {
        timesCalled++;
    }

    public void assertCalledTimes( int expectedTimesCalled )
    {
        assertEquals( expectedTimesCalled, timesCalled, "Callback was not called the correct number of times" );
    }

    public void assertNotCalled()
    {
        assertEquals( 0, timesCalled, "Should never have been called." );
    }

    public void reset()
    {
        this.timesCalled = 0;
    }
}
