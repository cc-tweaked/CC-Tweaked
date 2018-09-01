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
import org.squiddev.cobalt.LuaError;
import org.squiddev.cobalt.LuaState;
import org.squiddev.cobalt.OrphanedThread;
import org.squiddev.cobalt.Varargs;
import org.squiddev.cobalt.function.VarArgFunction;

class CobaltWrapperFunction extends VarArgFunction
{
    private final CobaltLuaMachine machine;
    private final Computer computer;

    private final ILuaObject delegate;
    private final int method;
    private final String methodName;

    public CobaltWrapperFunction( CobaltLuaMachine machine, Computer computer, ILuaObject delegate, int method, String methodName )
    {
        this.machine = machine;
        this.computer = computer;
        this.delegate = delegate;
        this.method = method;
        this.methodName = methodName;
    }

    @Override
    public Varargs invoke( final LuaState state, Varargs _args ) throws LuaError
    {
        Object[] arguments = CobaltLuaMachine.toObjects( _args, 1 );
        Object[] results;
        try
        {
            results = delegate.callMethod( new CobaltLuaContext( machine, computer, state ), method, arguments );
        }
        catch( InterruptedException e )
        {
            throw new OrphanedThread();
        }
        catch( LuaException e )
        {
            throw new LuaError( e.getMessage(), e.getLevel() );
        }
        catch( Throwable t )
        {
            if( ComputerCraft.logPeripheralErrors )
            {
                ComputerCraft.log.error( "Error calling " + methodName + " on " + delegate, t );
            }
            throw new LuaError( "Java Exception Thrown: " + t.toString(), 0 );
        }
        return machine.toValues( results );
    }
}
