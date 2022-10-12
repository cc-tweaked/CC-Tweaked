/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.core.computer;

import dan200.computercraft.api.lua.ILuaAPI;
import dan200.computercraft.core.lua.ILuaMachine;
import dan200.computercraft.core.lua.MachineResult;
import dan200.computercraft.core.terminal.Terminal;
import dan200.computercraft.support.IsolatedRunner;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
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
 *
 * Note, this will clobber some parts of the global state. It's recommended you use this inside an {@link IsolatedRunner}.
 */
public class FakeComputerManager
{
    interface Task
    {
        MachineResult run( TimeoutState state ) throws Exception;
    }

    private static final Map<Computer, Queue<Task>> machines = new HashMap<>();

    private static final Lock errorLock = new ReentrantLock();
    private static final Condition hasError = errorLock.newCondition();
    private static volatile Throwable error;

    static
    {
        ComputerExecutor.luaFactory = ( computer, timeout ) -> new DummyLuaMachine( timeout, machines.get( computer ) );
    }

    /**
     * Create a new computer which pulls from our task queue.
     *
     * @return The computer. This will not be started yet, you must call {@link Computer#turnOn()} and
     * {@link Computer#tick()} to do so.
     */
    @Nonnull
    public static Computer create()
    {
        Computer computer = new Computer( new BasicEnvironment(), new Terminal( 51, 19 ), 0 );
        machines.put( computer, new ConcurrentLinkedQueue<>() );
        return computer;
    }

    /**
     * Create and start a new computer which loops forever.
     */
    public static void createLoopingComputer()
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
    public static void enqueue( @Nonnull Computer computer, @Nonnull Task task )
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
    private static void enqueueForever( @Nonnull Computer computer, @Nonnull Task task )
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
    public static void sleep( long delay, TimeUnit unit ) throws Exception
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
    public static void startAndWait( Computer computer ) throws Exception
    {
        computer.turnOn();
        computer.tick();

        do
        {
            sleep( 100, TimeUnit.MILLISECONDS );
        } while( ComputerThread.hasPendingWork() || computer.isOn() );

        rethrowIfNeeded();
    }

    private static void rethrowIfNeeded() throws Exception
    {
        if( error == null ) return;
        if( error instanceof Exception ) throw (Exception) error;
        if( error instanceof Error ) throw (Error) error;
        rethrow( error );
    }

    @SuppressWarnings( "unchecked" )
    private static <T extends Throwable> void rethrow( Throwable e ) throws T
    {
        throw (T) e;
    }

    private static class DummyLuaMachine implements ILuaMachine
    {
        private final TimeoutState state;
        private final Queue<Task> handleEvent;

        DummyLuaMachine( TimeoutState state, Queue<Task> handleEvent )
        {
            this.state = state;
            this.handleEvent = handleEvent;
        }

        @Override
        public void addAPI( @Nonnull ILuaAPI api )
        {
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
                return handleEvent.remove().run( state );
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
