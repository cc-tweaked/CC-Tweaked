/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2018. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.core.lua;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.api.lua.ILuaFunction;
import dan200.computercraft.api.lua.ILuaObject;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.MethodResult;
import dan200.computercraft.core.computer.Computer;
import org.squiddev.cobalt.*;
import org.squiddev.cobalt.debug.DebugFrame;
import org.squiddev.cobalt.debug.DebugHandler;
import org.squiddev.cobalt.debug.DebugState;
import org.squiddev.cobalt.function.VarArgFunction;

import java.util.ArrayDeque;
import java.util.Deque;

class CobaltWrapperFunction extends VarArgFunction implements Resumable<CobaltWrapperFunction.State>
{
    private final CobaltLuaMachine machine;
    private final Computer computer;
    private final CobaltCallContext callContext;

    private final ILuaObject delegate;
    private final int method;
    private final String methodName;

    CobaltWrapperFunction( CobaltLuaMachine machine, Computer computer, ILuaObject delegate, int method, String methodName )
    {
        this.machine = machine;
        this.computer = computer;
        this.callContext = new CobaltCallContext( computer );
        this.delegate = delegate;
        this.method = method;
        this.methodName = methodName;
    }

    @Override
    public Varargs invoke( final LuaState state, Varargs args ) throws LuaError, UnwindThrowable
    {
        MethodResult future;
        try
        {
            future = delegate.callMethod( callContext, method, CobaltLuaMachine.toObjects( args, 1 ) );
        }
        catch( LuaException e )
        {
            throw new LuaError( e.getMessage(), e.getLevel() );
        }
        catch( Exception e )
        {
            if( ComputerCraft.logPeripheralErrors )
            {
                ComputerCraft.log.error( "Error calling " + methodName + " on " + delegate, e );
            }
            throw new LuaError( "Java Exception Thrown: " + e.toString(), 0 );
        }

        if( future == null )
        {
            ComputerCraft.log.error( "Null result from " + delegate );
            throw new LuaError( "Java Exception Thrown: Null result" );
        }

        State context = new State();
        try
        {
            return runFuture( state, context, future );
        }
        catch( UnwindThrowable e )
        {
            // Push our state onto the stack if need-be.
            DebugHandler handler = state.debug;
            DebugState ds = handler.getDebugState();
            DebugFrame di = handler.onCall( ds, this );
            di.state = context;

            throw e;
        }
    }

    @Override
    public Varargs resume( LuaState state, State context, Varargs args ) throws LuaError, UnwindThrowable
    {
        try
        {
            Varargs result = doResume( state, context, args );
            state.debug.onReturn();
            return result;
        }
        catch( LuaError e )
        {
            state.debug.onReturn();
            throw e;
        }
        catch( Exception e )
        {
            state.debug.onReturn();
            throw new LuaError( e );
        }
    }

    private Varargs doResume( LuaState state, State context, Varargs args ) throws LuaError, UnwindThrowable
    {
        MethodResult future = context.pending;
        if( future instanceof MethodResult.OnEvent )
        {
            MethodResult.OnEvent onEvent = (MethodResult.OnEvent) future;
            if( !onEvent.isRaw() && args.first().toString().equals( "terminate" ) )
            {
                throw new LuaError( "Terminated", 0 );
            }

            return runCallback( state, context, CobaltLuaMachine.toObjects( args, 1 ) );
        }
        else if( future instanceof MethodResult.OnMainThread )
        {
            if( args.arg( 2 ).isNumber() && args.arg( 3 ).isBoolean() && args.arg( 2 ).toLong() == context.taskId )
            {
                if( args.arg( 3 ).toBoolean() )
                {
                    // Extract the return values from the event and return them
                    return runCallback( state, context, CobaltLuaMachine.toObjects( args, 4 ) );
                }
                else
                {
                    // Extract the error message from the event and raise it
                    throw new LuaError( args.arg( 4 ) );
                }
            }
            else
            {
                LuaThread.yield( state, ValueFactory.valueOf( "task_complete" ) );
                throw new IllegalStateException( "Unreachable" );
            }
        }
        else if( future instanceof MethodResult.WithLuaContext )
        {
            return runCallback( state, context, context.luaContext.resume( state, machine, CobaltLuaMachine.toObjects( args, 1 ) ) );
        }
        else
        {
            ComputerCraft.log.error( "Unknown method result " + future );
            throw new LuaError( "Java Exception Thrown: Unknown method result" );
        }
    }

    @Override
    public Varargs resumeError( LuaState state, State context, LuaError error ) throws LuaError
    {
        state.debug.onReturn();
        throw error;
    }

    private Varargs runFuture( LuaState state, State context, MethodResult future ) throws LuaError, UnwindThrowable
    {
        Deque<ILuaFunction> callbacks = context.callbacks;
        while( true )
        {
            if( future instanceof MethodResult.AndThen )
            {
                MethodResult.AndThen then = ((MethodResult.AndThen) future);

                // Thens are "unwrapped", being pushed onto a stack
                if( callbacks == null ) callbacks = context.callbacks = new ArrayDeque<>();
                callbacks.addLast( then.getCallback() );

                future = then.getPrevious();
            }
            else if( future instanceof MethodResult.Immediate )
            {
                Object[] values = ((MethodResult.Immediate) future).getResult();

                // Immediate values values will attempt to call the previous "then", or return if nothing 
                // else needs to be done.
                ILuaFunction callback = callbacks == null ? null : callbacks.pollLast();
                if( callback == null ) return machine.toValues( values );

                future = runFunction( callback, values );
            }
            else if( future instanceof MethodResult.OnEvent )
            {
                MethodResult.OnEvent onEvent = (MethodResult.OnEvent) future;

                // Mark this future as pending and yield
                context.pending = future;
                String filter = onEvent.getFilter();
                LuaThread.yield( state, filter == null ? Constants.NIL : ValueFactory.valueOf( filter ) );
                throw new IllegalStateException( "Unreachable" );
            }
            else if( future instanceof MethodResult.OnMainThread )
            {
                MethodResult.OnMainThread onMainThread = (MethodResult.OnMainThread) future;

                // Mark this future as pending and yield
                context.pending = future;
                try
                {
                    context.taskId = callContext.issueMainThreadTask( () -> {
                        context.taskResult = onMainThread.getTask().execute();
                        return null;
                    } );
                }
                catch( LuaException e )
                {
                    throw new LuaError( e.getMessage(), e.getLevel() );
                }

                LuaThread.yield( state, ValueFactory.valueOf( "task_complete" ) );
                throw new IllegalStateException( "Unreachable" );
            }
            else if( future instanceof MethodResult.WithLuaContext )
            {
                MethodResult.WithLuaContext withContext = (MethodResult.WithLuaContext) future;

                // Mark this future as pending and execute on a separate thread.
                context.pending = future;
                CobaltLuaContext luaContext = context.luaContext = new CobaltLuaContext( computer, state );
                luaContext.execute( withContext.getConsumer() );
            }
            else
            {
                ComputerCraft.log.error( "Unknown method result " + future );
                throw new LuaError( "Java Exception Thrown: Unknown method result" );
            }
        }
    }

    private Varargs runCallback( LuaState state, State context, Object[] args ) throws LuaError, UnwindThrowable
    {
        Deque<ILuaFunction> callbacks = context.callbacks;
        ILuaFunction callback = callbacks == null ? null : callbacks.pollLast();
        if( callback == null ) return machine.toValues( args );

        return runFuture( state, context, runFunction( callback, args ) );
    }

    private MethodResult runFunction( ILuaFunction func, Object[] args ) throws LuaError
    {
        MethodResult result;
        try
        {
            result = func.call( args );
        }
        catch( LuaException e )
        {
            throw new LuaError( e.getMessage(), e.getLevel() );
        }
        catch( Exception e )
        {
            if( ComputerCraft.logPeripheralErrors )
            {
                ComputerCraft.log.error( "Error calling " + methodName + " on " + delegate, e );
            }
            throw new LuaError( "Java Exception Thrown: " + e.toString(), 0 );
        }

        if( result == null )
        {
            ComputerCraft.log.error( "Null result from " + func );
            throw new LuaError( "Java Exception Thrown: Null result" );
        }

        return result;
    }

    static class State
    {
        Deque<ILuaFunction> callbacks;

        MethodResult pending;

        CobaltLuaContext luaContext;

        long taskId;
        MethodResult taskResult;
    }
}
