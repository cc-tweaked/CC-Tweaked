/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.core.lua;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.api.lua.*;
import dan200.computercraft.core.asm.LuaMethod;
import org.squiddev.cobalt.*;
import org.squiddev.cobalt.debug.DebugFrame;
import org.squiddev.cobalt.function.ResumableVarArgFunction;

import javax.annotation.Nonnull;

/**
 * Calls a {@link LuaMethod}, and interprets the resulting {@link MethodResult}, either returning the result or yielding
 * and resuming the supplied continuation.
 */
class ResultInterpreterFunction extends ResumableVarArgFunction<ResultInterpreterFunction.Container>
{
    @Nonnull
    static class Container
    {
        ILuaCallback callback;
        final int errorAdjust;

        Container( ILuaCallback callback, int errorAdjust )
        {
            this.callback = callback;
            this.errorAdjust = errorAdjust;
        }
    }

    private final CobaltLuaMachine machine;
    private final LuaMethod method;
    private final Object instance;
    private final ILuaContext context;
    private final String name;

    ResultInterpreterFunction( CobaltLuaMachine machine, LuaMethod method, Object instance, ILuaContext context, String name )
    {
        this.machine = machine;
        this.method = method;
        this.instance = instance;
        this.context = context;
        this.name = name;
    }

    @Override
    protected Varargs invoke( LuaState state, DebugFrame debugFrame, Varargs args ) throws LuaError, UnwindThrowable
    {
        IArguments arguments = CobaltLuaMachine.toArguments( args );
        MethodResult results;
        try
        {
            results = method.apply( instance, context, arguments );
        }
        catch( LuaException e )
        {
            throw wrap( e, 0 );
        }
        catch( Throwable t )
        {
            if( ComputerCraft.logComputerErrors )
            {
                ComputerCraft.log.error( "Error calling " + name + " on " + instance, t );
            }
            throw new LuaError( "Java Exception Thrown: " + t, 0 );
        }
        finally
        {
            arguments.releaseImmediate();
        }

        ILuaCallback callback = results.getCallback();
        Varargs ret = machine.toValues( results.getResult() );

        if( callback == null ) return ret;

        debugFrame.state = new Container( callback, results.getErrorAdjust() );
        return LuaThread.yield( state, ret );
    }

    @Override
    protected Varargs resumeThis( LuaState state, Container container, Varargs args ) throws LuaError, UnwindThrowable
    {
        MethodResult results;
        Object[] arguments = CobaltLuaMachine.toObjects( args );
        try
        {
            results = container.callback.resume( arguments );
        }
        catch( LuaException e )
        {
            throw wrap( e, container.errorAdjust );
        }
        catch( Throwable t )
        {
            if( ComputerCraft.logComputerErrors )
            {
                ComputerCraft.log.error( "Error calling " + name + " on " + container.callback, t );
            }
            throw new LuaError( "Java Exception Thrown: " + t, 0 );
        }

        Varargs ret = machine.toValues( results.getResult() );

        ILuaCallback callback = results.getCallback();
        if( callback == null ) return ret;

        container.callback = callback;
        return LuaThread.yield( state, ret );
    }

    public static LuaError wrap( LuaException exception, int adjust )
    {
        if( !exception.hasLevel() && adjust == 0 ) return new LuaError( exception.getMessage() );

        int level = exception.getLevel();
        return new LuaError( exception.getMessage(), level <= 0 ? level : level + adjust + 1 );
    }
}
