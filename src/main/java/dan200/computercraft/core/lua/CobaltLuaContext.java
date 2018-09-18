/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2018. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.core.lua;

import dan200.computercraft.api.lua.ILuaContext;
import dan200.computercraft.api.lua.ILuaContextTask;
import dan200.computercraft.api.lua.ILuaTask;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.core.computer.Computer;
import org.squiddev.cobalt.LuaError;
import org.squiddev.cobalt.LuaState;
import org.squiddev.cobalt.LuaThread;
import org.squiddev.cobalt.UnwindThrowable;

import javax.annotation.Nonnull;
import java.lang.ref.WeakReference;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * An ugly wrapper for {@link ILuaContext} style calls, which executes them on a separate thread.
 */
class CobaltLuaContext extends CobaltCallContext implements ILuaContext
{
    private static final ThreadGroup group = new ThreadGroup( "ComputerCraft-Lua" );
    private static final AtomicInteger threadCounter = new AtomicInteger();
    private static final ExecutorService threads = new ThreadPoolExecutor(
        4, Integer.MAX_VALUE, 60L, TimeUnit.SECONDS, new SynchronousQueue<>(),
        task -> {
            Thread thread = new Thread( group, task, group.getName() + "-" + threadCounter.incrementAndGet() );
            if( !thread.isDaemon() ) thread.setDaemon( true );
            if( thread.getPriority() != Thread.NORM_PRIORITY ) thread.setPriority( Thread.NORM_PRIORITY );
            return thread;
        }
    );

    private boolean done = false;
    private Object[] values;
    private LuaError exception;
    private final Semaphore yield = new Semaphore();
    private final Semaphore resume = new Semaphore();
    private WeakReference<LuaThread> thread;

    CobaltLuaContext( Computer computer, LuaState state )
    {
        super( computer );
        this.thread = state.getCurrentThread().getReference();
    }

    @Nonnull
    @Override
    @Deprecated
    public Object[] pullEvent( String filter ) throws LuaException, InterruptedException
    {
        Object[] results = pullEventRaw( filter );
        if( results.length >= 1 && results[0].equals( "terminate" ) )
        {
            throw new LuaException( "Terminated", 0 );
        }
        return results;
    }

    @Nonnull
    @Override
    @Deprecated
    public Object[] pullEventRaw( String filter ) throws InterruptedException
    {
        return yield( new Object[]{ filter } );
    }

    @Nonnull
    @Override
    @Deprecated
    public Object[] yield( Object[] yieldArgs ) throws InterruptedException
    {
        if( done ) throw new IllegalStateException( "Cannot yield when complete" );

        values = yieldArgs;
        yield.signal();

        // Every 30 seconds check to see if the coroutine has been GCed
        // if so then abort this task.
        while( !resume.await( 30000 ) )
        {
            if( thread.get() == null ) throw new InterruptedException( "Orphaned async task" );
        }

        return values;
    }

    @Override
    @Deprecated
    public Object[] executeMainThreadTask( @Nonnull final ILuaTask task ) throws LuaException, InterruptedException
    {
        // Issue task
        final long taskID = issueMainThreadTask( task );

        // Wait for response
        while( true )
        {
            Object[] response = pullEvent( "task_complete" );
            if( response.length >= 3 && response[1] instanceof Number && response[2] instanceof Boolean )
            {
                if( ((Number) response[1]).intValue() == taskID )
                {
                    Object[] returnValues = new Object[response.length - 3];
                    if( (Boolean) response[2] )
                    {
                        // Extract the return values from the event and return them
                        System.arraycopy( response, 3, returnValues, 0, returnValues.length );
                        return returnValues;
                    }
                    else
                    {
                        // Extract the error message from the event and raise it
                        if( response.length >= 4 && response[3] instanceof String )
                        {
                            throw new LuaException( (String) response[3] );
                        }
                        else
                        {
                            throw new LuaException();
                        }
                    }
                }
            }
        }
    }

    void execute( ILuaContextTask task )
    {
        threads.submit( () -> {
            try
            {
                values = task.execute( this );
            }
            catch( LuaException e )
            {
                exception = new LuaError( e.getMessage(), e.getLevel() );
            }
            catch( InterruptedException e )
            {
                exception = new LuaError( "Java Exception Thrown: " + e.toString(), 0 );
            }
            finally
            {
                done = true;
                yield.signal();
            }
        } );
    }

    void resume( Object[] args )
    {
        values = args;
        resume.signal();
    }

    Object[] await( LuaState state, CobaltLuaMachine machine ) throws LuaError, UnwindThrowable
    {
        if( !done )
        {
            try
            {
                yield.await();
            }
            catch( InterruptedException e )
            {
                throw new LuaError( "Java Exception Thrown: " + e.toString(), 0 );
            }
        }

        if( done )
        {
            if( exception != null ) throw exception;
            return values;
        }
        else
        {
            LuaThread.yield( state, machine.toValues( values ) );
            throw new IllegalStateException( "Unreachable" );
        }
    }
}
