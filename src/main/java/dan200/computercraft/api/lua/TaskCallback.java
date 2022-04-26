/*
 * This file is part of the public ComputerCraft API - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. This API may be redistributed unmodified and in full only.
 * For help using the API, and posting your mods, visit the forums at computercraft.info.
 */
package dan200.computercraft.api.lua;

import javax.annotation.Nonnull;
import java.util.Arrays;

final class TaskCallback implements ILuaCallback
{
    private final MethodResult pull = MethodResult.pullEvent( "task_complete", this );
    private final long task;

    private TaskCallback( long task )
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

    static MethodResult make( ILuaContext context, ILuaTask func ) throws LuaException
    {
        long task = context.issueMainThreadTask( func );
        return new TaskCallback( task ).pull;
    }
}
