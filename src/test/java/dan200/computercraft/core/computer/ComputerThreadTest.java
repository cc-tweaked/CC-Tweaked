/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.core.computer;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.core.lua.MachineResult;
import dan200.computercraft.support.ConcurrentHelpers;
import dan200.computercraft.support.IsolatedRunner;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import java.util.concurrent.TimeUnit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.closeTo;
import static org.junit.jupiter.api.Assertions.*;

@Timeout( value = 15 )
@ExtendWith( IsolatedRunner.class )
@Execution( ExecutionMode.CONCURRENT )
public class ComputerThreadTest
{
    @Test
    public void testSoftAbort() throws Exception
    {
        Computer computer = FakeComputerManager.create();
        FakeComputerManager.enqueue( computer, timeout -> {
            assertFalse( timeout.isSoftAborted(), "Should not start soft-aborted" );

            long delay = ConcurrentHelpers.waitUntil( timeout::isSoftAborted );
            assertThat( "Should be soft aborted", delay * 1e-9, closeTo( 7, 0.5 ) );
            ComputerCraft.log.info( "Slept for {}", delay );

            computer.shutdown();
            return MachineResult.OK;
        } );

        FakeComputerManager.startAndWait( computer );
    }

    @Test
    public void testHardAbort() throws Exception
    {
        Computer computer = FakeComputerManager.create();
        FakeComputerManager.enqueue( computer, timeout -> {
            assertFalse( timeout.isHardAborted(), "Should not start soft-aborted" );

            assertThrows( InterruptedException.class, () -> Thread.sleep( 11_000 ), "Sleep should be hard aborted" );
            assertTrue( timeout.isHardAborted(), "Thread should be hard aborted" );

            computer.shutdown();
            return MachineResult.OK;
        } );

        FakeComputerManager.startAndWait( computer );
    }

    @Test
    public void testNoPauseIfNoOtherMachines() throws Exception
    {
        Computer computer = FakeComputerManager.create();
        FakeComputerManager.enqueue( computer, timeout -> {
            boolean didPause = ConcurrentHelpers.waitUntil( timeout::isPaused, 5, TimeUnit.SECONDS );
            assertFalse( didPause, "Machine shouldn't have paused within 5s" );

            computer.shutdown();
            return MachineResult.OK;
        } );

        FakeComputerManager.startAndWait( computer );
    }

    @Test
    public void testPauseIfSomeOtherMachine() throws Exception
    {
        Computer computer = FakeComputerManager.create();
        FakeComputerManager.enqueue( computer, timeout -> {
            long budget = ComputerThread.scaledPeriod();
            assertEquals( budget, TimeUnit.MILLISECONDS.toNanos( 25 ), "Budget should be 25ms" );

            long delay = ConcurrentHelpers.waitUntil( timeout::isPaused );
            assertThat( "Paused within 25ms", delay * 1e-9, closeTo( 0.03, 0.015 ) );

            computer.shutdown();
            return MachineResult.OK;
        } );

        FakeComputerManager.createLoopingComputer();

        FakeComputerManager.startAndWait( computer );
    }
}
