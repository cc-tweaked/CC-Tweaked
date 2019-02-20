/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.core.computer;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.core.tracking.Tracking;
import dan200.computercraft.shared.util.ThreadUtils;

import java.util.HashSet;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.locks.LockSupport;

public class ComputerThread
{
    private static final int QUEUE_LIMIT = 256;

    /**
     * Lock used for modifications to the object
     */
    private static final Object s_stateLock = new Object();

    /**
     * Lock for various task operations
     */
    private static final Object s_taskLock = new Object();

    /**
     * Map of objects to task list
     */
    private static final WeakHashMap<Object, BlockingQueue<ITask>> s_computerTaskQueues = new WeakHashMap<>();

    /**
     * Active queues to execute
     */
    private static final BlockingQueue<BlockingQueue<ITask>> s_computerTasksActive = new LinkedBlockingQueue<>();
    private static final Set<BlockingQueue<ITask>> s_computerTasksActiveSet = new HashSet<>();

    /**
     * The default object for items which don't have an owner
     */
    private static final Object s_defaultOwner = new Object();

    /**
     * Whether the thread is stopped or should be stopped
     */
    private static boolean s_stopped = false;

    /**
     * The thread tasks execute on
     */
    private static Thread[] s_threads = null;

    private static final ThreadFactory s_ManagerFactory = ThreadUtils.factory( "Computer-Manager" );
    private static final ThreadFactory s_RunnerFactory = ThreadUtils.factory( "Computer-Runner" );

    /**
     * Start the computer thread
     */
    public static void start()
    {
        synchronized( s_stateLock )
        {
            s_stopped = false;
            if( s_threads == null || s_threads.length != ComputerCraft.computer_threads )
            {
                s_threads = new Thread[ComputerCraft.computer_threads];
            }

            for( int i = 0; i < s_threads.length; i++ )
            {
                Thread thread = s_threads[i];
                if( thread == null || !thread.isAlive() )
                {
                    (s_threads[i] = s_ManagerFactory.newThread( new TaskExecutor() )).start();
                }
            }
        }
    }

    /**
     * Attempt to stop the computer thread
     */
    public static void stop()
    {
        synchronized( s_stateLock )
        {
            if( s_threads != null )
            {
                s_stopped = true;
                for( Thread thread : s_threads )
                {
                    if( thread != null && thread.isAlive() )
                    {
                        thread.interrupt();
                    }
                }
            }
        }

        synchronized( s_taskLock )
        {
            s_computerTaskQueues.clear();
            s_computerTasksActive.clear();
            s_computerTasksActiveSet.clear();
        }
    }

    /**
     * Queue a task to execute on the thread
     *
     * @param task     The task to execute
     * @param computer The computer to execute it on, use {@code null} to execute on the default object.
     */
    public static void queueTask( ITask task, Computer computer )
    {
        Object queueObject = computer == null ? s_defaultOwner : computer;

        BlockingQueue<ITask> queue;
        synchronized( s_computerTaskQueues )
        {
            queue = s_computerTaskQueues.get( queueObject );
            if( queue == null )
            {
                s_computerTaskQueues.put( queueObject, queue = new LinkedBlockingQueue<>( QUEUE_LIMIT ) );
            }
        }

        synchronized( s_taskLock )
        {
            if( queue.offer( task ) && !s_computerTasksActiveSet.contains( queue ) )
            {
                s_computerTasksActive.add( queue );
                s_computerTasksActiveSet.add( queue );
            }
        }
    }

    /**
     * Responsible for pulling and managing computer tasks. This pulls a task from {@link #s_computerTasksActive},
     * creates a new thread using {@link TaskRunner} or reuses a previous one and uses that to execute the task.
     *
     * If the task times out, then it will attempt to interrupt the {@link TaskRunner} instance.
     */
    private static final class TaskExecutor implements Runnable
    {
        private TaskRunner runner;
        private Thread thread;

        @Override
        public void run()
        {
            try
            {
                while( true )
                {
                    // Wait for an active queue to execute
                    BlockingQueue<ITask> queue = s_computerTasksActive.take();

                    // If threads should be stopped then return
                    synchronized( s_stateLock )
                    {
                        if( s_stopped ) return;
                    }

                    execute( queue );
                }
            }
            catch( InterruptedException ignored )
            {
                Thread.currentThread().interrupt();
            }
        }

        private void execute( BlockingQueue<ITask> queue ) throws InterruptedException
        {
            ITask task = queue.remove();

            if( thread == null || !thread.isAlive() )
            {
                runner = new TaskRunner();
                (thread = s_RunnerFactory.newThread( runner )).start();
            }

            long start = System.nanoTime();

            // Execute the task
            runner.submit( task );

            try
            {
                // If we ran within our time period, then just exit
                if( runner.await( 7000 ) ) return;

                Computer computer = task.getOwner();

                // Attempt to soft then hard abort
                computer.timeout.softAbort();
                if( runner.await( 1500 ) ) return;

                computer.timeout.hardAbort();
                if( runner.await( 1500 ) ) return;

                if( ComputerCraft.logPeripheralErrors )
                {
                    long time = System.nanoTime() - start;
                    StringBuilder builder = new StringBuilder()
                        .append( "Terminating computer #" ).append( computer.getID() )
                        .append( " due to timeout (running for " ).append( time / 1e9 )
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

                // Interrupt the thread
                thread.interrupt();
                thread = null;
                runner = null;
            }
            finally
            {
                long stop = System.nanoTime();

                Computer computer = task.getOwner();
                Tracking.addTaskTiming( computer, stop - start );

                computer.timeout.resetAbort();

                // Re-add it back onto the queue or remove it
                synchronized( s_taskLock )
                {
                    if( queue.isEmpty() )
                    {
                        s_computerTasksActiveSet.remove( queue );
                    }
                    else
                    {
                        s_computerTasksActive.add( queue );
                    }
                }
            }
        }
    }

    /**
     * Responsible for the actual running of tasks. It waitin for the {@link TaskRunner#input} semaphore to be
     * triggered, consumes a task and then triggers {@link TaskRunner#finished}.
     */
    private static final class TaskRunner implements Runnable
    {
        private final Semaphore input = new Semaphore();
        private final Semaphore finished = new Semaphore();
        private ITask task;

        @Override
        public void run()
        {
            try
            {
                while( true )
                {
                    input.await();
                    try
                    {
                        task.execute();
                    }
                    catch( RuntimeException e )
                    {
                        ComputerCraft.log.error( "Error running task.", e );
                    }
                    task = null;
                    finished.signal();
                }
            }
            catch( InterruptedException e )
            {
                ComputerCraft.log.error( "Error running task.", e );
                Thread.currentThread().interrupt();
            }
        }

        void submit( ITask task )
        {
            this.task = task;
            input.signal();
        }

        boolean await( long timeout ) throws InterruptedException
        {
            return finished.await( timeout );
        }
    }

    /**
     * A simple method to allow awaiting/providing a signal.
     *
     * Java does provide similar classes, but I only needed something simple.
     */
    private static final class Semaphore
    {
        private volatile boolean state = false;

        synchronized void signal()
        {
            state = true;
            notify();
        }

        synchronized void await() throws InterruptedException
        {
            while( !state ) wait();
            state = false;
        }

        synchronized boolean await( long timeout ) throws InterruptedException
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
}
