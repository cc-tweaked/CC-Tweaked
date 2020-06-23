/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.core.asm;

import dan200.computercraft.api.lua.ILuaCallback;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.MethodResult;

import javax.annotation.Nonnull;
import java.util.Arrays;

class TaskCallback implements ILuaCallback
{
    final MethodResult pull = MethodResult.pullEvent( "task_complete", this );
    private final long task;

    TaskCallback( long task )
    {
        this.task = task;
    }

    @Nonnull
    @Override
    public MethodResult resume( Object[] response ) throws LuaException
    {
        if( response.length < 3 || !(response[1] instanceof Number) || !(response[2] instanceof Boolean) )
        {
            return pull;
        }

        if( ((Number) response[1]).longValue() != task ) return pull;

        if( (Boolean) response[2] )
        {
            // Extract the return values from the event and return them
            return MethodResult.of( Arrays.copyOfRange( response, 3, response.length ) );
        }
        else if( response.length >= 4 && response[3] instanceof String )
        {
            // Extract the error message from the event and raise it
            throw new LuaException( (String) response[3] );
        }
        else
        {
            throw new LuaException( "error" );
        }
    }

    public static Object[] checkUnwrap( MethodResult result )
    {
        if( result.getCallback() != null ) throw new IllegalStateException( "Cannot return MethodResult currently" );
        return result.getResult();
    }
}
