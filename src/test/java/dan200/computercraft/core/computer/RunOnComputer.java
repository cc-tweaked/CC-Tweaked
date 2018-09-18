/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2018. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.core.computer;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.api.lua.*;
import dan200.computercraft.core.terminal.Terminal;
import org.apache.logging.log4j.LogManager;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.function.Consumer;

import static org.junit.Assert.fail;

public final class RunOnComputer
{
    public static final int STARTUP_TIMEOUT = 10;
    public static final int RUN_TIMEOUT = 100;

    public static void run( String task ) throws Exception
    {
        run( task, x -> {
        } );
    }

    public static void run( String task, Consumer<Computer> setup ) throws Exception
    {
        if( ComputerCraft.log == null ) ComputerCraft.log = LogManager.getLogger( "computercraft" );
        ComputerCraft.logPeripheralErrors = true;

        // Setup computer
        Terminal terminal = new Terminal( ComputerCraft.terminalWidth_computer, ComputerCraft.terminalHeight_computer );
        Computer computer = new Computer( new FakeComputerEnvironment( 0, true ), terminal, 0 );

        // Register APIS
        TestAPI api = new TestAPI( computer );
        computer.addAPI( api );
        setup.accept( computer );

        // Setup the startup file
        try( OutputStream stream = computer.getRootMount().openForWrite( "startup.lua" ) )
        {
            String program = "" +
                "local function exec()\n" +
                "  " + task + "\n" +
                "end\n" +
                "test.finish(pcall(exec))\n";
            stream.write( program.getBytes( StandardCharsets.UTF_8 ) );
        }

        // Turn on
        ComputerThread.start();
        computer.turnOn();

        // Run until shutdown or we timeout
        boolean everOn = false;
        int ticks = 0;
        do
        {
            computer.advance( 0.05 );
            MainThread.executePendingTasks();

            Thread.sleep( 50 );
            ticks++;
            everOn |= computer.isOn();
        }
        while( (computer.isOn() || (!everOn && ticks < STARTUP_TIMEOUT)) && ticks <= RUN_TIMEOUT );

        // If we never finished (say, startup errored) then print the terminal. Otherwise throw the error
        // where needed.
        if( !api.finished )
        {
            int height = terminal.getHeight() - 1;
            while( height >= 0 && terminal.getLine( height ).toString().trim().isEmpty() ) height--;
            for( int y = 0; y <= height; y++ )
            {
                System.out.printf( "%2d | %s\n", y + 1, terminal.getLine( y ) );
            }

            fail( "Computer never finished" );
        }
        else if( api.error != null )
        {
            fail( api.error );
        }

        ComputerThread.stop();
    }

    private static class TestAPI implements ILuaAPI
    {
        private final Computer computer;

        private boolean finished = false;
        private String error;

        private TestAPI( Computer computer )
        {
            this.computer = computer;
        }

        @Override
        public String[] getNames()
        {
            return new String[]{ "test" };
        }

        @Nonnull
        @Override
        public String[] getMethodNames()
        {
            return new String[]{ "log", "finish" };
        }

        @Nullable
        @Override
        @Deprecated
        public Object[] callMethod( @Nonnull ILuaContext context, int method, @Nonnull Object[] arguments ) throws LuaException, InterruptedException
        {
            return callMethod( (ICallContext) context, method, arguments ).evaluate( context );
        }

        @Nonnull
        @Override
        public MethodResult callMethod( @Nonnull ICallContext context, int method, @Nonnull Object[] arguments )
        {
            switch( method )
            {
                case 0:
                    ComputerCraft.log.info( Objects.toString( arguments.length <= 0 ? null : arguments[0] ) );
                    return MethodResult.empty();
                case 1:
                {
                    if( arguments.length <= 0 || arguments[0] == null || arguments[0] == Boolean.FALSE )
                    {
                        error = Objects.toString( arguments.length <= 1 ? null : arguments[1] );
                    }
                    finished = true;
                    computer.shutdown();
                    return MethodResult.empty();
                }
                default:
                    return MethodResult.empty();
            }
        }
    }
}
