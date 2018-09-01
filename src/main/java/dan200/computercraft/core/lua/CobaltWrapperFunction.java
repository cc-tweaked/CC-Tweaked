/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2018. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.core.lua;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.api.lua.ILuaObject;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.core.computer.Computer;
import org.squiddev.cobalt.*;
import org.squiddev.cobalt.debug.DebugFrame;
import org.squiddev.cobalt.debug.DebugHandler;
import org.squiddev.cobalt.debug.DebugState;
import org.squiddev.cobalt.function.VarArgFunction;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

class CobaltWrapperFunction extends VarArgFunction implements Resumable<CobaltLuaContext>
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

    private final CobaltLuaMachine machine;
    private final Computer computer;

    private final ILuaObject delegate;
    private final int method;
    private final String methodName;

    CobaltWrapperFunction( CobaltLuaMachine machine, Computer computer, ILuaObject delegate, int method, String methodName )
    {
        this.machine = machine;
        this.computer = computer;
        this.delegate = delegate;
        this.method = method;
        this.methodName = methodName;
    }

    @Override
    public Varargs invoke( final LuaState state, Varargs args ) throws LuaError, UnwindThrowable
    {
        Object[] arguments = CobaltLuaMachine.toObjects( args, 1 );
        CobaltLuaContext context = new CobaltLuaContext( computer, state );

        DebugHandler handler = state.debug;
        DebugState ds = handler.getDebugState();
        DebugFrame di = handler.onCall( ds, this );
        di.state = context;

        threads.submit( () -> {
            try
            {
                context.values = delegate.callMethod( context, method, arguments );
            }
            catch( LuaException e )
            {
                context.exception = new LuaError( e );
            }
            catch( InterruptedException e )
            {
                if( ComputerCraft.logPeripheralErrors )
                {
                    ComputerCraft.log.error( "Error calling " + methodName + " on " + delegate, e );
                }
                context.exception = new LuaError( "Java Exception Thrown: " + e.toString(), 0 );
            }
            finally
            {
                context.done = true;
                context.yield.signal();
            }
        } );

        try
        {
            return handleResult( state, context );
        }
        catch( InterruptedException e )
        {
            throw new LuaError( e );
        }
    }

    @Override
    public Varargs resume( LuaState state, CobaltLuaContext context, Varargs value ) throws LuaError, UnwindThrowable
    {
        context.values = CobaltLuaMachine.toObjects( value, 0 );
        context.resume.signal();
        try
        {
            return handleResult( state, context );
        }
        catch( InterruptedException e )
        {
            throw new LuaError( e );
        }
    }

    @Override
    public Varargs resumeError( LuaState state, CobaltLuaContext context, LuaError error ) throws LuaError
    {
        DebugHandler handler = state.debug;
        handler.onReturn( handler.getDebugState() );
        throw error;
    }

    private Varargs handleResult( LuaState state, CobaltLuaContext context ) throws InterruptedException, LuaError, UnwindThrowable
    {
        // We may be done if we yield when handling errors
        if( !context.done ) context.yield.await();

        if( context.done )
        {
            if( context.exception != null )
            {
                context.exception.fillTraceback( state );
                state.debug.onReturn();
                throw context.exception;
            }
            else
            {
                state.debug.onReturn();
                return machine.toValues( context.values );
            }
        }
        else
        {
            LuaThread.yield( state, machine.toValues( context.values ) );
            throw new AssertionError( "Unreachable code" );
        }
    }
}
