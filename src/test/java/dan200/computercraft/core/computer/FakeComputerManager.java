/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.core.computer;

import dan200.computercraft.api.lua.ILuaAPI;
import dan200.computercraft.core.ComputerContext;
import dan200.computercraft.core.lua.ILuaMachine;
import dan200.computercraft.core.lua.MachineResult;
import dan200.computercraft.core.terminal.Terminal;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Creates "fake" computers, which just run user-defined tasks rather than Lua code.
 */
public class FakeComputerManager implements AutoCloseable
{
    interface Task
    {
        MachineResult run( TimeoutState state ) throws Exception;
    }

    private final Map<Computer, Queue<Task>> machines = new HashMap<>();
    private final ComputerContext context = new ComputerContext(
        new BasicEnvironment(),
        new ComputerThread( 1 ),
        new FakeMainThreadScheduler(),
        args -> new DummyLuaMachine( args.timeout )
    );

    private final Lock errorLock = new ReentrantLock();
    private final Condition hasError = errorLock.newCondition();
    private volatile @Nullable Throwable error;

    @Override
    public void close()
    {
        try
        {
            context.ensureClosed( 1, TimeUnit.SECONDS );
        }
        catch( InterruptedException e )
        {
            throw new IllegalStateException( "Runtime thread was interrupted", e );
        }
    }

    public ComputerContext context()
    {
        return context;
    }

    /**
     * Create a new computer which pulls from our task queue.
     *
     * @return The computer. This will not be started yet, you must call {@link Computer#turnOn()} and
     * {@link Computer#tick()} to do so.
     */
    public Computer create()
    {
        Queue<Task> queue = new ConcurrentLinkedQueue<>();
        Computer computer = new Computer( context, new BasicEnvironment(), new Terminal( 51, 19, true ), 0 );
        computer.addApi( new QueuePassingAPI( queue ) ); // Inject an extra API to pass the queue to the machine.
        machines.put( computer, queue );
        return computer;
    }

    /**
     * Create and start a new computer which loops forever.
     */
    public void createLoopingComputer()
    {
        Computer computer = create();
        enqueueForever( computer, t -> {
            Thread.sleep( 100 );
            return MachineResult.OK;
        } );
        computer.turnOn();
        computer.tick();
    }

    /**
     * Enqueue a task on a computer.
     *
     * @param computer The computer to enqueue the work on.
     * @param task     The task to run.
     */
    public void enqueue( Computer computer, Task task )
    {
        machines.get( computer ).offer( task );
    }

    /**
     * Enqueue a repeated task on a computer. This is automatically requeued when the task finishes, meaning the task
     * queue is never empty.
     *
     * @param computer The computer to enqueue the work on.
     * @param task     The task to run.
     */
    private void enqueueForever( Computer computer, Task task )
    {
        machines.get( computer ).offer( t -> {
            MachineResult result = task.run( t );

            enqueueForever( computer, task );
            computer.queueEvent( "some_event", null );
            return result;
        } );
    }

    /**
     * Sleep for a given period, immediately propagating any exceptions thrown by a computer.
     *
     * @param delay The duration to sleep for.
     * @param unit  The time unit the duration is measured in.
     * @throws Exception An exception thrown by a running computer.
     */
    public void sleep( long delay, TimeUnit unit ) throws Exception
    {
        errorLock.lock();
        try
        {
            rethrowIfNeeded();
            if( hasError.await( delay, unit ) ) rethrowIfNeeded();
        }
        finally
        {
            errorLock.unlock();
        }
    }

    /**
     * Start a computer and wait for it to finish.
     *
     * @param computer The computer to wait for.
     * @throws Exception An exception thrown by a running computer.
     */
    public void startAndWait( Computer computer ) throws Exception
    {
        computer.turnOn();
        computer.tick();

        do
        {
            sleep( 100, TimeUnit.MILLISECONDS );
        } while( context.computerScheduler().hasPendingWork() || computer.isOn() );

        rethrowIfNeeded();
    }

    private void rethrowIfNeeded() throws Exception
    {
        Throwable error = this.error;
        if( error == null ) return;
        if( error instanceof Exception ) throw (Exception) error;
        rethrow( error );
    }

    @SuppressWarnings( "unchecked" )
    private static <T extends Throwable> void rethrow( Throwable e ) throws T
    {
        throw (T) e;
    }

    private static final class QueuePassingAPI implements ILuaAPI
    {
        final Queue<Task> tasks;

        private QueuePassingAPI( Queue<Task> tasks )
        {
            this.tasks = tasks;
        }

        @Override
        public String[] getNames()
        {
            return new String[0];
        }
    }

    private final class DummyLuaMachine implements ILuaMachine
    {
        private final TimeoutState state;
        private @Nullable Queue<Task> tasks;

        DummyLuaMachine( TimeoutState state )
        {
            this.state = state;
        }

        @Override
        public void addAPI( @Nonnull ILuaAPI api )
        {
            if( api instanceof QueuePassingAPI ) tasks = ((QueuePassingAPI) api).tasks;
        }

        @Override
        public MachineResult loadBios( @Nonnull InputStream bios )
        {
            return MachineResult.OK;
        }

        @Override
        public MachineResult handleEvent( @Nullable String eventName, @Nullable Object[] arguments )
        {
            try
            {
                if( tasks == null ) throw new IllegalStateException( "Not received tasks yet" );
                return tasks.remove().run( state );
            }
            catch( Throwable e )
            {
                errorLock.lock();
                try
                {
                    if( error == null )
                    {
                        error = e;
                        hasError.signal();
                    }
                    else
                    {
                        error.addSuppressed( e );
                    }
                }
                finally
                {
                    errorLock.unlock();
                }

                if( !(e instanceof Exception) && !(e instanceof AssertionError) ) rethrow( e );
                return MachineResult.error( e.getMessage() );
            }
        }

        @Override
        public void printExecutionState( StringBuilder out )
        {
        }

        @Override
        public void close()
        {
        }
    }
}
