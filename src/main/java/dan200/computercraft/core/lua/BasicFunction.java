/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.core.lua;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.api.lua.IArguments;
import dan200.computercraft.api.lua.ILuaContext;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.MethodResult;
import dan200.computercraft.core.asm.LuaMethod;
import org.squiddev.cobalt.LuaError;
import org.squiddev.cobalt.LuaState;
import org.squiddev.cobalt.Varargs;
import org.squiddev.cobalt.function.VarArgFunction;

/**
 * An "optimised" version of {@link ResultInterpreterFunction} which is guaranteed to never yield.
 *
 * As we never yield, we do not need to push a function to the stack, which removes a small amount of overhead.
 */
class BasicFunction extends VarArgFunction
{
    private final CobaltLuaMachine machine;
    private final LuaMethod method;
    private final Object instance;
    private final ILuaContext context;
    private final String name;

    BasicFunction( CobaltLuaMachine machine, LuaMethod method, Object instance, ILuaContext context, String name )
    {
        this.machine = machine;
        this.method = method;
        this.instance = instance;
        this.context = context;
        this.name = name;
    }

    @Override
    public Varargs invoke( LuaState luaState, Varargs args ) throws LuaError
    {
        IArguments arguments = CobaltLuaMachine.toArguments( args );
        MethodResult results;
        try
        {
            results = method.apply( instance, context, arguments );
        }
        catch( LuaException e )
        {
            throw wrap( e );
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

        if( results.getCallback() != null )
        {
            throw new IllegalStateException( "Cannot have a yielding non-yielding function" );
        }
        return machine.toValues( results.getResult() );
    }

    public static LuaError wrap( LuaException exception )
    {
        return exception.hasLevel() ? new LuaError( exception.getMessage() ) : new LuaError( exception.getMessage(), exception.getLevel() );
    }
}
