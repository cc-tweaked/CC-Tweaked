/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2018. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.core.lua;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.api.lua.ILuaContext;
import dan200.computercraft.api.lua.ILuaTask;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.core.computer.Computer;
import dan200.computercraft.core.computer.ITask;
import dan200.computercraft.core.computer.MainThread;
import org.squiddev.cobalt.LuaState;
import org.squiddev.cobalt.LuaThread;
import org.squiddev.cobalt.OrphanedThread;
import org.squiddev.cobalt.Varargs;

import javax.annotation.Nonnull;

class CobaltLuaContext implements ILuaContext
{
    private CobaltLuaMachine machine;
    private final Computer computer;
    private final LuaState state;

    public CobaltLuaContext( CobaltLuaMachine machine, Computer computer, LuaState state )
    {
        this.machine = machine;
        this.computer = computer;
        this.state = state;
    }

    @Nonnull
    @Override
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
    public Object[] pullEventRaw( String filter ) throws InterruptedException
    {
        return yield( new Object[]{ filter } );
    }

    @Nonnull
    @Override
    public Object[] yield( Object[] yieldArgs ) throws InterruptedException
    {
        try
        {
            Varargs results = LuaThread.yield( state, machine.toValues( yieldArgs ) );
            return CobaltLuaMachine.toObjects( results, 1 );
        }
        catch( OrphanedThread e )
        {
            throw new InterruptedException();
        }
        catch( Throwable e )
        {
            throw new RuntimeException( e );
        }
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

    @Override
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
}
