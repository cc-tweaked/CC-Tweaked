/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2018. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.core.lua;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.api.lua.ICallContext;
import dan200.computercraft.api.lua.ILuaTask;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.core.computer.Computer;
import dan200.computercraft.core.computer.ITask;
import dan200.computercraft.core.computer.MainThread;

import javax.annotation.Nonnull;

class CobaltCallContext implements ICallContext
{
    private final Computer computer;

    CobaltCallContext( Computer computer )
    {
        this.computer = computer;
    }

    @Override
    public long issueMainThreadTask( @Nonnull final ILuaTask task ) throws LuaException
    {
        // Issue command
        final long taskID = MainThread.getUniqueTaskID();
        final ITask iTask = new ITask()
        {
            @Override
            public Computer getOwner()
            {
                return computer;
            }

            @Override
            public void execute()
            {
                try
                {
                    Object[] results = task.execute();
                    if( results != null )
                    {
                        Object[] eventArguments = new Object[results.length + 2];
                        eventArguments[0] = taskID;
                        eventArguments[1] = true;
                        System.arraycopy( results, 0, eventArguments, 2, results.length );
                        computer.queueEvent( "task_complete", eventArguments );
                    }
                    else
                    {
                        computer.queueEvent( "task_complete", new Object[]{ taskID, true } );
                    }
                }
                catch( LuaException e )
                {
                    computer.queueEvent( "task_complete", new Object[]{
                        taskID, false, e.getMessage()
                    } );
                }
                catch( Throwable t )
                {
                    if( ComputerCraft.logPeripheralErrors )
                    {
                        ComputerCraft.log.error( "Error running task", t );
                    }
                    computer.queueEvent( "task_complete", new Object[]{
                        taskID, false, "Java Exception Thrown: " + t.toString()
                    } );
                }
            }
        };
        if( MainThread.queueTask( iTask ) )
        {
            return taskID;
        }
        else
        {
            throw new LuaException( "Task limit exceeded" );
        }
    }
}
