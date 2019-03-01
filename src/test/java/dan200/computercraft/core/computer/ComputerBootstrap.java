/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.core.computer;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.api.lua.ILuaAPI;
import dan200.computercraft.api.lua.ILuaContext;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.core.apis.ArgumentHelper;
import dan200.computercraft.core.filesystem.MemoryMount;
import dan200.computercraft.core.terminal.Terminal;
import org.apache.logging.log4j.LogManager;
import org.junit.jupiter.api.Assertions;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Helper class to run a program on a computer.
 */
public class ComputerBootstrap
{
    private static final int TPS = 20;
    private static final int MAX_TIME = 10;

    public static void run( String program )
    {
        run( program, -1 );
    }

    public static void run( String program, int shutdownAfter )
    {
        ComputerCraft.logPeripheralErrors = true;
        ComputerCraft.log = LogManager.getLogger( ComputerCraft.MOD_ID );

        MemoryMount mount = new MemoryMount()
            .addFile( "test.lua", program )
            .addFile( "startup", "assertion.assert(pcall(loadfile('test.lua', _ENV))) os.shutdown()" );

        Terminal term = new Terminal( ComputerCraft.terminalWidth_computer, ComputerCraft.terminalHeight_computer );
        final Computer computer = new Computer( new BasicEnvironment( mount ), term, 0 );

        AssertApi api = new AssertApi();
        computer.addApi( api );

        try
        {
            computer.turnOn();
            boolean everOn = false;

            for( int tick = 0; tick < TPS * MAX_TIME; tick++ )
            {
                long start = System.currentTimeMillis();

                computer.tick();
                MainThread.executePendingTasks();

                if( api.message != null )
                {
                    ComputerCraft.log.debug( "Shutting down due to error" );
                    computer.shutdown();
                    Assertions.fail( api.message );
                    return;
                }

                long remaining = (1000 / TPS) - (System.currentTimeMillis() - start);
                if( remaining > 0 ) Thread.sleep( remaining );

                // Break if the computer was once on, and is now off.
                everOn |= computer.isOn();
                if( (everOn || tick > TPS) && !computer.isOn() ) break;

                // Shutdown the computer after a period of time
                if( shutdownAfter > 0 && tick != 0 && tick % shutdownAfter == 0 )
                {
                    ComputerCraft.log.info( "Shutting down: shutdown after {}", shutdownAfter );
                    computer.shutdown();
                }
            }

            if( computer.isOn() || !api.didAssert )
            {
                StringBuilder builder = new StringBuilder().append( "Did not correctly [" );
                if( !api.didAssert ) builder.append( " assert" );
                if( computer.isOn() ) builder.append( " shutdown" );
                builder.append( " ]\n" );

                for( int line = 0; line < 19; line++ )
                {
                    builder.append( String.format( "%2d | %" + term.getWidth() + "s |\n", line + 1, term.getLine( line ) ) );
                }

                computer.shutdown();
                Assertions.fail( builder.toString() );
            }
        }
        catch( InterruptedException ignored )
        {
            Thread.currentThread().interrupt();
        }
    }

    private static class AssertApi implements ILuaAPI
    {
        boolean didAssert;
        String message;

        @Override
        public String[] getNames()
        {
            return new String[] { "assertion" };
        }

        @Nonnull
        @Override
        public String[] getMethodNames()
        {
            return new String[] { "assert" };
        }

        @Nullable
        @Override
        public Object[] callMethod( @Nonnull ILuaContext context, int method, @Nonnull Object[] arguments ) throws LuaException, InterruptedException
        {
            switch( method )
            {
                case 0: // assert
                {
                    didAssert = true;

                    Object arg = arguments.length >= 1 ? arguments[0] : null;
                    if( arg == null || arg == Boolean.FALSE )
                    {
                        message = ArgumentHelper.optString( arguments, 1, "Assertion failed" );
                        throw new LuaException( message );
                    }

                    return arguments;
                }

                default:
                    return null;
            }
        }
    }
}
