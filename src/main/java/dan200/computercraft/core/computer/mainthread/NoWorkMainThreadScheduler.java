/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.core.computer.mainthread;

import dan200.computercraft.core.metrics.MetricsObserver;

import javax.annotation.Nonnull;
import java.util.concurrent.TimeUnit;

/**
 * A {@link MainThreadScheduler} which fails when a computer tries to enqueue work.
 * <p>
 * This is useful for emulators, where we'll never make any main thread calls.
 */
public class NoWorkMainThreadScheduler implements MainThreadScheduler
{
    @Override
    public Executor createExecutor( MetricsObserver observer )
    {
        return new ExecutorImpl();
    }

    private static class ExecutorImpl implements Executor
    {
        @Override
        public boolean enqueue( Runnable task )
        {
            throw new IllegalStateException( "Cannot schedule tasks" );
        }

        @Override
        public boolean canWork()
        {
            return false;
        }

        @Override
        public boolean shouldWork()
        {
            return false;
        }

        @Override
        public void trackWork( long time, @Nonnull TimeUnit unit )
        {
        }
    }
}
