/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.core.computer;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.shared.util.ThreadUtils;

import javax.annotation.Nonnull;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;

import static dan200.computercraft.core.computer.TimeoutState.ABORT_TIMEOUT;
import static dan200.computercraft.core.computer.TimeoutState.TIMEOUT;

/**
 * Responsible for running all tasks from a {@link Computer}.
 *
 * This is split into two components: the {@link TaskRunner}s, which pull an executor from the queue and execute it, and
 * a single {@link Monitor} which observes all runners and kills them if they are behaving badly.
 */
public class ComputerThread
{
    /**
     * How often the computer thread monitor should run, in milliseconds
     *
     * @see Monitor
     */
    private static final int MONITOR_WAKEUP = 100;

    /**
     * Lock used for modifications to the array of current threads.
     */
    private static final Object threadLock = new Object();

    /**
     * Active executors to run
     */
    private static final BlockingQueue<ComputerExecutor> computersActive = new LinkedBlockingQueue<>();

    /**
     * Whether the computer thread system is currently running
     */
    private static volatile boolean running = false;

    /**
     * The current task manager.
     */
    private static Thread monitor;

    /**
     * The array of current runners, and their owning threads.
     */
    private static TaskRunner[] runners;

    private static final ThreadFactory monitorFactory = ThreadUtils.factory( "Computer-Monitor" );
    private static final ThreadFactory runnerFactory = ThreadUtils.factory( "Computer-Runner" );

    /**
     * Start the computer thread
     */
    static void start()
    {
        synchronized( threadLock )
        {
            running = true;
            if( monitor == null || !monitor.isAlive() ) (monitor = monitorFactory.newThread( new Monitor() )).start();

            if( runners == null || runners.length != ComputerCraft.computer_threads )
            {
                // TODO: Resize this + kill old runners and start new ones.
                runners = new TaskRunner[ComputerCraft.computer_threads];
            }

            for( int i = 0; i < runners.length; i++ )
            {
                TaskRunner runner = runners[i];
                if( runner == null || runner.owner == null || !runner.owner.isAlive() )
                {
                    // Mark the old runner as dead, just in case.
                    if( runner != null ) runner.running = false;
                    // And start a new runner
                    runnerFactory.newThread( runners[i] = new TaskRunner() ).start();
                }
            }
        }
    }

    /**
     * Attempt to stop the computer thread. This interrupts each runner, and clears the task queue.
     */
    public static void stop()
    {
        synchronized( threadLock )
        {
            running = false;
            if( runners != null )
            {
                for( TaskRunner runner : runners )
                {
                    if( runner == null ) continue;

                    runner.running = false;
                    if( runner.owner != null ) runner.owner.interrupt();
                }
            }
        }

        computersActive.clear();
    }

    /**
     * Mark a computer as having work, enqueuing it on the thread.
     *
     * @param computer The computer to execute work on.
     */
    static void queue( @Nonnull ComputerExecutor computer )
    {
        if( !computer.onComputerQueue ) throw new IllegalStateException( "Computer must be on queue" );
        computersActive.add( computer );
    }

    /**
     * Observes all currently active {@link TaskRunner}s and terminates their tasks once they have exceeded the hard
     * abort limit.
     *
     * @see TimeoutState
     */
    private static final class Monitor implements Runnable
    {
        @Override
        public void run()
        {
            try
            {
                while( true )
                {
                    Thread.sleep( MONITOR_WAKEUP );

                    TaskRunner[] currentRunners = ComputerThread.runners;
                    if( currentRunners != null )
                    {
                        for( int i = 0; i < currentRunners.length; i++ )
                        {
                            TaskRunner runner = currentRunners[i];
                            // If we've no runner, skip.
                            if( runner == null ) continue;

                            // If the runner has no work, skip
                            ComputerExecutor executor = runner.currentExecutor.get();
                            if( executor == null ) continue;

                            // If we're still within normal execution times (TIMEOUT) or soft abort (ABORT_TIMEOUT),
                            // then we can let the Lua machine do its work.
                            long afterStart = executor.timeout.nanoSinceStart();
                            long afterHardAbort = afterStart - TIMEOUT - ABORT_TIMEOUT;
                            if( afterHardAbort < 0 ) continue;

                            // Set the hard abort flag.
                            executor.timeout.hardAbort();
                            executor.abort();

                            if( afterHardAbort >= ABORT_TIMEOUT * 2 )
                            {
                                // If we've hard aborted and interrupted, and we're still not dead, then mark the runner
                                // as dead, finish off the task, and spawn a new runner.
                                // Note, we'll do the actual interruption of the thread in the next block.
                                runner.running = false;

                                ComputerExecutor thisExecutor = runner.currentExecutor.getAndSet( null );
                                if( thisExecutor != null ) executor.afterWork();

                                synchronized( threadLock )
                                {
                                    if( running && runners.length > i && runners[i] == runner )
                                    {
                                        runnerFactory.newThread( currentRunners[i] = new TaskRunner() ).start();
                                    }
                                }
                            }

                            if( afterHardAbort >= ABORT_TIMEOUT )
                            {
                                // If we've hard aborted but we're still not dead, dump the stack trace and interrupt
                                // the task.
                                timeoutTask( executor, runner.owner, afterStart );
                                runner.owner.interrupt();
                            }
                        }
                    }
                }
            }
            catch( InterruptedException ignored )
            {
            }
        }
    }

    /**
     * Pulls tasks from the {@link #computersActive} queue and runs them.
     *
     * This is responsible for running the {@link ComputerExecutor#work()}, {@link ComputerExecutor#beforeWork()} and
     * {@link ComputerExecutor#afterWork()} functions. Everything else is either handled by the executor, timeout
     * state or monitor.
     */
    private static final class TaskRunner implements Runnable
    {
        Thread owner;
        volatile boolean running = true;

        final AtomicReference<ComputerExecutor> currentExecutor = new AtomicReference<>();

        @Override
        public void run()
        {
            owner = Thread.currentThread();

            while( running && ComputerThread.running )
            {
                // Wait for an active queue to execute
                ComputerExecutor executor;
                try
                {
                    executor = computersActive.take();
                }
                catch( InterruptedException ignored )
                {
                    // If we've been interrupted, our running flag has probably been reset, so we'll
                    // just jump into the next iteration.
                    continue;
                }

                // Pull a task from this queue, and set what we're currently executing.
                currentExecutor.set( executor );

                // Execute the task
                executor.beforeWork();
                try
                {
                    executor.work();
                }
                catch( Exception e )
                {
                    ComputerCraft.log.error( "Error running task on computer #" + executor.getComputer().getID(), e );
                }
                finally
                {
                    ComputerExecutor thisExecutor = currentExecutor.getAndSet( null );
                    if( thisExecutor != null ) executor.afterWork();
                }
            }
        }
    }

    private static void timeoutTask( ComputerExecutor executor, Thread thread, long nanotime )
    {
        if( !ComputerCraft.logPeripheralErrors ) return;

        StringBuilder builder = new StringBuilder()
            .append( "Terminating computer #" ).append( executor.getComputer().getID() )
            .append( " due to timeout (running for " ).append( nanotime / 1e9 )
            .append( " seconds). This is NOT a bug, but may mean a computer is misbehaving. " )
            .append( thread.getName() )
            .append( " is currently " )
            .append( thread.getState() );
        Object blocking = LockSupport.getBlocker( thread );
        if( blocking != null ) builder.append( "\n  on " ).append( blocking );

        for( StackTraceElement element : thread.getStackTrace() )
        {
            builder.append( "\n  at " ).append( element );
        }

        ComputerCraft.log.warn( builder.toString() );
    }
}
