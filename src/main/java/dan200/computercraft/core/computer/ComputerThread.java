/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.core.computer;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.shared.util.ThreadUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.TreeSet;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.LockSupport;
import java.util.concurrent.locks.ReentrantLock;

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
     * The target latency between executing two tasks on a single machine.
     *
     * An average tick takes 50ms, and so we ideally need to have handled a couple of events within that window in order
     * to have a perceived low latency.
     */
    private static final long DEFAULT_LATENCY = TimeUnit.MILLISECONDS.toNanos( 50 );

    /**
     * The minimum value that {@link #DEFAULT_LATENCY} can have when scaled.
     *
     * From statistics gathered on SwitchCraft, almost all machines will execute under 15ms, 75% under 1.5ms, with the
     * mean being about 3ms. Most computers shouldn't be too impacted with having such a short period to execute in.
     */
    private static final long DEFAULT_MIN_PERIOD = TimeUnit.MILLISECONDS.toNanos( 5 );

    /**
     * The maximum number of tasks before we have to start scaling latency linearly.
     */
    private static final long LATENCY_MAX_TASKS = DEFAULT_LATENCY / DEFAULT_MIN_PERIOD;

    /**
     * Lock used for modifications to the array of current threads.
     */
    private static final Object threadLock = new Object();

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

    private static long latency;
    private static long minPeriod;

    private static final ReentrantLock computerLock = new ReentrantLock();

    private static final Condition hasWork = computerLock.newCondition();

    /**
     * Active queues to execute
     */
    private static final TreeSet<ComputerExecutor> computerQueue = new TreeSet<>( ( a, b ) -> {
        if( a == b ) return 0; // Should never happen, but let's be consistent here

        long at = a.virtualRuntime, bt = b.virtualRuntime;
        if( at == bt ) return Integer.compare( a.hashCode(), b.hashCode() );
        return Long.compare( at, bt );
    } );

    /**
     * The minimum {@link ComputerExecutor#virtualRuntime} time on the tree.
     */
    private static long minimumVirtualRuntime = 0;

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

            if( runners == null )
            {
                // TODO: Change the runners lenght on config reloads
                runners = new TaskRunner[ComputerCraft.computer_threads];

                // latency and minPeriod are scaled by 1 + floor(log2(threads)). We can afford to execute tasks for
                // longer when executing on more than one thread.
                long factor = 64 - Long.numberOfLeadingZeros( runners.length );
                latency = DEFAULT_LATENCY * factor;
                minPeriod = DEFAULT_MIN_PERIOD * factor;
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

        computerLock.lock();
        try
        {
            computerQueue.clear();
        }
        finally
        {
            computerLock.unlock();
        }
    }

    /**
     * Mark a computer as having work, enqueuing it on the thread.
     *
     * @param executor The computer to execute work on.
     */
    static void queue( @Nonnull ComputerExecutor executor )
    {
        computerLock.lock();
        try
        {
            if( executor.onComputerQueue ) throw new IllegalStateException( "Cannot queue already queued executor" );
            executor.onComputerQueue = true;

            updateRuntimes( null );

            // We're not currently on the queue, so update its current execution time to
            // ensure its at least as high as the minimum.
            long newRuntime = minimumVirtualRuntime;

            if( executor.virtualRuntime == 0 )
            {
                // Slow down new computers a little bit.
                newRuntime += scaledPeriod();
            }
            else
            {
                // Give a small boost to computers which have slept a little.
                newRuntime -= latency / 2;
            }

            executor.virtualRuntime = Math.max( newRuntime, executor.virtualRuntime );

            // Add to the queue, and signal the workers.
            computerQueue.add( executor );
            hasWork.signal();
        }
        finally
        {
            computerLock.unlock();
        }
    }


    /**
     * Update the {@link ComputerExecutor#virtualRuntime}s of all running tasks, and then increment the
     * {@link #minimumVirtualRuntime} of the executor.
     */
    private static void updateRuntimes( @Nullable ComputerExecutor current )
    {
        long minRuntime = Long.MAX_VALUE;

        // If we've a task on the queue, use that as our base time.
        if( !computerQueue.isEmpty() ) minRuntime = computerQueue.first().virtualRuntime;

        long now = System.nanoTime();
        int tasks = 1 + computerQueue.size();

        // Update all the currently executing tasks
        TaskRunner[] currentRunners = runners;
        if( currentRunners != null )
        {
            for( TaskRunner runner : currentRunners )
            {
                if( runner == null ) continue;
                ComputerExecutor executor = runner.currentExecutor.get();
                if( executor == null ) continue;

                // We do two things here: first we update the task's virtual runtime based on when we
                // last checked, and then we check the minimum.
                minRuntime = Math.min( minRuntime, executor.virtualRuntime += (now - executor.vRuntimeStart) / tasks );
                executor.vRuntimeStart = now;
            }
        }

        // And update the most recently executed one if set.
        if( current != null )
        {
            minRuntime = Math.min( minRuntime, current.virtualRuntime += (now - current.vRuntimeStart) / tasks );
        }

        if( minRuntime > minimumVirtualRuntime && minRuntime < Long.MAX_VALUE )
        {
            minimumVirtualRuntime = minRuntime;
        }
    }

    /**
     * Re-add this task to the queue
     *
     * @param executor The executor to requeue
     */
    private static void afterWork( ComputerExecutor executor )
    {
        computerLock.lock();
        try
        {
            updateRuntimes( executor );

            // Add to the queue, and signal the workers.
            if( !executor.afterWork() ) return;

            computerQueue.add( executor );
            hasWork.signal();
        }
        finally
        {
            computerLock.unlock();
        }
    }

    /**
     * The scaled period for a single task
     *
     * @return The scaled period for the task
     * @see #DEFAULT_LATENCY
     * @see #DEFAULT_MIN_PERIOD
     * @see #LATENCY_MAX_TASKS
     */
    static long scaledPeriod()
    {
        // +1 to include the current task
        int count = 1 + computerQueue.size();
        return count < LATENCY_MAX_TASKS ? latency / count : minPeriod;
    }

    /**
     * Determine if the thread has computers queued up
     *
     * @return If we have work queued up.
     */
    static boolean hasPendingWork()
    {
        return computerQueue.size() > 0;
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
                            long afterStart = executor.timeout.nanoCumulative();
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
                                if( thisExecutor != null ) afterWork( executor );

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
                                timeoutTask( executor, runner.owner );
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
     * Pulls tasks from the {@link #computerQueue} queue and runs them.
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
                    computerLock.lockInterruptibly();
                    try
                    {
                        while( computerQueue.isEmpty() ) hasWork.await();
                        executor = computerQueue.pollFirst();
                        assert executor != null : "hasWork should ensure we never receive null work";
                    }
                    finally
                    {
                        computerLock.unlock();
                    }
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
                    if( thisExecutor != null ) afterWork( executor );
                }
            }
        }
    }

    private static void timeoutTask( ComputerExecutor executor, Thread thread )
    {
        if( !ComputerCraft.logPeripheralErrors ) return;

        StringBuilder builder = new StringBuilder()
            .append( "Terminating computer #" ).append( executor.getComputer().getID() )
            .append( " due to timeout (running for " ).append( executor.timeout.nanoCumulative() * 1e-9 )
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
