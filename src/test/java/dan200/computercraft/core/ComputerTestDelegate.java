/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.core;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.api.filesystem.IWritableMount;
import dan200.computercraft.api.lua.ILuaAPI;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.core.computer.BasicEnvironment;
import dan200.computercraft.core.computer.Computer;
import dan200.computercraft.core.computer.ComputerSide;
import dan200.computercraft.core.computer.MainThread;
import dan200.computercraft.core.filesystem.FileMount;
import dan200.computercraft.core.filesystem.FileSystemException;
import dan200.computercraft.core.terminal.Terminal;
import dan200.computercraft.shared.peripheral.modem.ModemState;
import dan200.computercraft.shared.peripheral.modem.wireless.WirelessModemPeripheral;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.function.Executable;
import org.opentest4j.AssertionFailedError;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static dan200.computercraft.api.lua.LuaValues.getType;

/**
 * Loads tests from {@code test-rom/spec} and executes them.
 *
 * This spins up a new computer and runs the {@code mcfly.lua} script. This will then load all files in the {@code spec}
 * directory and register them with {@code cct_test.start}.
 *
 * From the test names, we generate a tree of {@link DynamicNode}s which queue an event and wait for
 * {@code cct_test.submit} to be called. McFly pulls these events, executes the tests and then calls the submit method.
 *
 * Once all tests are done, we invoke {@code cct_test.finish} in order to mark everything as complete.
 */
public class ComputerTestDelegate
{
    private static final File REPORT_PATH = new File( "test-files/luacov.report.out" );

    private static final Logger LOG = LogManager.getLogger( ComputerTestDelegate.class );

    private static final long TICK_TIME = TimeUnit.MILLISECONDS.toNanos( 50 );

    private static final long TIMEOUT = TimeUnit.SECONDS.toNanos( 10 );

    private static final Set<String> SKIP_KEYWORDS = new HashSet<>(
        Arrays.asList( System.getProperty( "cc.skip_keywords", "" ).split( "," ) )
    );

    private static final Pattern KEYWORD = Pattern.compile( ":([a-z_]+)" );

    private final ReentrantLock lock = new ReentrantLock();
    private Computer computer;

    private final Condition hasTests = lock.newCondition();
    private DynamicNodeBuilder tests;

    private final Condition hasRun = lock.newCondition();
    private String currentTest;
    private boolean runFinished;
    private Throwable runResult;

    private final Condition hasFinished = lock.newCondition();
    private boolean finished = false;
    private Map<String, Map<Double, Double>> finishedWith;

    @BeforeEach
    public void before() throws IOException
    {
        ComputerCraft.logComputerErrors = true;

        if( REPORT_PATH.delete() ) ComputerCraft.log.info( "Deleted previous coverage report." );

        Terminal term = new Terminal( 80, 100 );
        IWritableMount mount = new FileMount( new File( "test-files/mount" ), 10_000_000 );

        // Remove any existing files
        List<String> children = new ArrayList<>();
        mount.list( "", children );
        for( String child : children ) mount.delete( child );

        // And add our startup file
        try( WritableByteChannel channel = mount.openForWrite( "startup.lua" );
             Writer writer = Channels.newWriter( channel, StandardCharsets.UTF_8.newEncoder(), -1 ) )
        {
            writer.write( "loadfile('test-rom/mcfly.lua', nil, _ENV)('test-rom/spec') cct_test.finish()" );
        }

        computer = new Computer( new BasicEnvironment( mount ), term, 0 );
        computer.getEnvironment().setPeripheral( ComputerSide.TOP, new FakeModem() );
        computer.addApi( new CctTestAPI() );

        computer.turnOn();
    }

    @AfterEach
    public void after() throws InterruptedException, IOException
    {
        try
        {
            LOG.info( "Finished execution" );
            computer.queueEvent( "cct_test_run", null );

            // Wait for test execution to fully finish
            lock.lockInterruptibly();
            try
            {
                long remaining = TIMEOUT;
                while( remaining > 0 && !finished )
                {
                    tick();
                    if( hasFinished.awaitNanos( TICK_TIME ) > 0 ) break;
                    remaining -= TICK_TIME;
                }

                if( remaining <= 0 ) throw new IllegalStateException( "Timed out waiting for finish." + dump() );
                if( !finished ) throw new IllegalStateException( "Computer did not finish." + dump() );
            }
            finally
            {
                lock.unlock();
            }
        }
        finally
        {
            // Show a dump of computer output
            System.out.println( dump() );

            // And shutdown
            computer.shutdown();
        }

        if( finishedWith != null )
        {
            REPORT_PATH.getParentFile().mkdirs();
            try( BufferedWriter writer = Files.newBufferedWriter( REPORT_PATH.toPath() ) )
            {
                new LuaCoverage( finishedWith ).write( writer );
            }
        }
    }

    @TestFactory
    public Stream<DynamicNode> get() throws InterruptedException
    {
        lock.lockInterruptibly();
        try
        {
            long remaining = TIMEOUT;
            while( remaining > 0 & tests == null )
            {
                tick();
                if( hasTests.awaitNanos( TICK_TIME ) > 0 ) break;
                remaining -= TICK_TIME;
            }

            if( remaining <= 0 ) throw new IllegalStateException( "Timed out waiting for tests. " + dump() );
            if( tests == null ) throw new IllegalStateException( "Computer did not provide any tests. " + dump() );
        }
        finally
        {
            lock.unlock();
        }

        return tests.buildChildren();
    }

    private static class DynamicNodeBuilder
    {
        private final String name;
        private final Map<String, DynamicNodeBuilder> children;
        private final Executable executor;

        DynamicNodeBuilder( String name )
        {
            this.name = name;
            this.children = new HashMap<>();
            this.executor = null;
        }

        DynamicNodeBuilder( String name, Executable executor )
        {
            this.name = name;
            this.children = Collections.emptyMap();
            this.executor = executor;
        }

        DynamicNodeBuilder get( String name )
        {
            DynamicNodeBuilder child = children.get( name );
            if( child == null ) children.put( name, child = new DynamicNodeBuilder( name ) );
            return child;
        }

        void runs( String name, Executable executor )
        {
            if( this.executor != null ) throw new IllegalStateException( name + " is leaf node" );
            if( children.containsKey( name ) ) throw new IllegalStateException( "Duplicate key for " + name );

            children.put( name, new DynamicNodeBuilder( name, executor ) );
        }

        boolean isActive()
        {
            Matcher matcher = KEYWORD.matcher( name );
            while( matcher.find() )
            {
                if( SKIP_KEYWORDS.contains( matcher.group( 1 ) ) ) return false;
            }

            return true;
        }

        DynamicNode build()
        {
            return executor == null
                ? DynamicContainer.dynamicContainer( name, buildChildren() )
                : DynamicTest.dynamicTest( name, executor );
        }

        Stream<DynamicNode> buildChildren()
        {
            return children.values().stream()
                .filter( DynamicNodeBuilder::isActive )
                .map( DynamicNodeBuilder::build );
        }
    }

    private String dump()
    {
        if( !computer.isOn() ) return "Computer is currently off.";

        Terminal term = computer.getAPIEnvironment().getTerminal();
        StringBuilder builder = new StringBuilder().append( "Computer is currently on.\n" );

        for( int line = 0; line < term.getHeight(); line++ )
        {
            builder.append( String.format( "%2d | %" + term.getWidth() + "s |\n", line + 1, term.getLine( line ) ) );
        }

        computer.shutdown();
        return builder.toString();
    }

    private void tick()
    {
        computer.tick();
        MainThread.executePendingTasks();
    }

    private static String formatName( String name )
    {
        return name.replace( "\0", " -> " );
    }

    private static class FakeModem extends WirelessModemPeripheral
    {
        FakeModem()
        {
            super( new ModemState(), true );
        }

        @Nonnull
        @Override
        @SuppressWarnings( "ConstantConditions" )
        public Level getLevel()
        {
            return null;
        }

        @Nonnull
        @Override
        public Vec3 getPosition()
        {
            return Vec3.ZERO;
        }

        @Override
        public boolean equals( @Nullable IPeripheral other )
        {
            return this == other;
        }
    }

    public class CctTestAPI implements ILuaAPI
    {
        @Override
        public String[] getNames()
        {
            return new String[] { "cct_test" };
        }

        @Override
        public void startup()
        {
            try
            {
                computer.getAPIEnvironment().getFileSystem().mount(
                    "test-rom", "test-rom",
                    BasicEnvironment.createMount( ComputerTestDelegate.class, "test-rom", "test" )
                );
            }
            catch( FileSystemException e )
            {
                throw new IllegalStateException( e );
            }
        }

        @LuaFunction
        public final void start( Map<?, ?> tests ) throws LuaException
        {
            // Submit several tests and signal for #get to run
            LOG.info( "Received tests from computer" );
            DynamicNodeBuilder root = new DynamicNodeBuilder( "" );
            for( Object key : tests.keySet() )
            {
                if( !(key instanceof String name) ) throw new LuaException( "Non-key string " + getType( key ) );

                String[] parts = name.split( "\0" );
                DynamicNodeBuilder builder = root;
                for( int i = 0; i < parts.length - 1; i++ ) builder = builder.get( parts[i] );
                builder.runs( parts[parts.length - 1], () -> {
                    // Run it
                    lock.lockInterruptibly();
                    try
                    {
                        // Set the current test
                        runResult = null;
                        runFinished = false;
                        currentTest = name;

                        // Tell the computer to run it
                        LOG.info( "Starting '{}'", formatName( name ) );
                        computer.queueEvent( "cct_test_run", new Object[] { name } );

                        long remaining = TIMEOUT;
                        while( remaining > 0 && computer.isOn() && !runFinished )
                        {
                            tick();

                            long waiting = hasRun.awaitNanos( TICK_TIME );
                            if( waiting > 0 ) break;
                            remaining -= TICK_TIME;
                        }

                        LOG.info( "Finished '{}'", formatName( name ) );

                        if( remaining <= 0 )
                        {
                            throw new IllegalStateException( "Timed out waiting for test" );
                        }
                        else if( !computer.isOn() )
                        {
                            throw new IllegalStateException( "Computer turned off mid-execution" );
                        }

                        if( runResult != null ) throw runResult;
                    }
                    finally
                    {
                        lock.unlock();
                        currentTest = null;
                    }
                } );
            }

            try
            {
                lock.lockInterruptibly();
            }
            catch( InterruptedException e )
            {
                throw new RuntimeException( e );
            }
            try
            {
                ComputerTestDelegate.this.tests = root;
                hasTests.signal();
            }
            finally
            {
                lock.unlock();
            }
        }

        @LuaFunction
        public final void submit( Map<?, ?> tbl )
        {
            //  Submit the result of a test, allowing the test executor to continue
            String name = (String) tbl.get( "name" );
            if( name == null )
            {
                ComputerCraft.log.error( "Oh no: {}", tbl );
            }
            String status = (String) tbl.get( "status" );
            String message = (String) tbl.get( "message" );
            String trace = (String) tbl.get( "trace" );

            StringBuilder wholeMessage = new StringBuilder();
            if( message != null ) wholeMessage.append( message );
            if( trace != null )
            {
                if( wholeMessage.length() != 0 ) wholeMessage.append( '\n' );
                wholeMessage.append( trace );
            }

            try
            {
                lock.lockInterruptibly();
            }
            catch( InterruptedException e )
            {
                throw new RuntimeException( e );
            }
            try
            {
                LOG.info( "'{}' finished with {}", formatName( name ), status );

                // Skip if a test mismatch
                if( !name.equals( currentTest ) )
                {
                    LOG.warn( "Skipping test '{}', as we're currently executing '{}'", formatName( name ), formatName( currentTest ) );
                    return;
                }

                switch( status )
                {
                    case "ok":
                    case "pending":
                        break;
                    case "fail":
                        runResult = new AssertionFailedError( wholeMessage.toString() );
                        break;
                    case "error":
                        runResult = new IllegalStateException( wholeMessage.toString() );
                        break;
                }

                runFinished = true;
                hasRun.signal();
            }
            finally
            {
                lock.unlock();
            }
        }

        @LuaFunction
        public final void finish( Optional<Map<?, ?>> result )
        {
            @SuppressWarnings( "unchecked" )
            Map<String, Map<Double, Double>> finishedResult = (Map<String, Map<Double, Double>>) result.orElse( null );
            LOG.info( "Finished" );

            // Signal to after that execution has finished
            try
            {
                lock.lockInterruptibly();
            }
            catch( InterruptedException e )
            {
                throw new RuntimeException( e );
            }
            try
            {
                finished = true;
                if( finishedResult != null ) finishedWith = finishedResult;

                hasFinished.signal();
            }
            finally
            {
                lock.unlock();
            }
        }
    }
}
