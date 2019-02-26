/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.core.computer;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.api.filesystem.IMount;
import dan200.computercraft.api.filesystem.IWritableMount;
import dan200.computercraft.api.lua.ILuaAPI;
import dan200.computercraft.api.lua.ILuaAPIFactory;
import dan200.computercraft.core.apis.*;
import dan200.computercraft.core.filesystem.FileSystem;
import dan200.computercraft.core.filesystem.FileSystemException;
import dan200.computercraft.core.lua.CobaltLuaMachine;
import dan200.computercraft.core.lua.ILuaMachine;
import dan200.computercraft.core.terminal.Terminal;
import dan200.computercraft.core.tracking.Tracking;
import dan200.computercraft.shared.util.IoUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.InputStream;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

/**
 * The main task queue and executor for a single computer. This handles turning on and off a computer, as well as
 * running events.
 *
 * When the computer is instructed to turn on or off, or handle an event, we queue a task and register this to be
 * executed on the {@link ComputerThread}. Note, as we may be starting many events in a single tick, the external
 * cannot lock on anything which may be held for a long time.
 *
 * The executor is effectively composed of two separate queues. Firstly, we have a "single element" queue
 * {@link #command} which determines which state the computer should transition too. This is set by
 * {@link #queueStart()} and {@link #queueStop(boolean, boolean)}.
 *
 * When a computer is on, we simply push any events onto to the {@link #eventQueue}.
 *
 * Both queues are run from the {@link #work()} method, which tries to execute a command if one exists, or resumes the
 * machine with an event otherwise.
 *
 * One final responsibility for the executor is calling {@link ILuaAPI#update()} every tick, via the {@link #tick()}
 * method. This should only be called when the computer is actually on ({@link #isOn}).
 */
final class ComputerExecutor
{
    private static final int QUEUE_LIMIT = 256;

    private static IMount romMount;
    private static final Object romMountLock = new Object();

    private final Computer computer;
    private final List<ILuaAPI> apis = new ArrayList<>();
    final TimeoutState timeout = new TimeoutState();

    private FileSystem fileSystem;

    private ILuaMachine machine;

    /**
     * Whether the computer is currently on. This is set to false when a shutdown starts, or when turning on completes
     * (but just before the Lua machine is started).
     *
     * @see #isOnLock
     */
    private volatile boolean isOn = false;

    /**
     * The lock to acquire when you need to modify the "on state" of a computer.
     *
     * We hold this lock when running any command, and attempt to hold it when updating APIs. This ensures you don't
     * update APIs while also starting/stopping them.
     *
     * @see #isOn
     * @see #tick()
     * @see #turnOn()
     * @see #shutdown()
     */
    private final ReentrantLock isOnLock = new ReentrantLock();

    /**
     * A lock used for any changes to {@link #eventQueue}, {@link #command} or {@link #onComputerQueue}. This will be
     * used on the main thread, so locks should be kept as brief as possible.
     */
    private final Object queueLock = new Object();

    /**
     * Determines if this executer is present within {@link ComputerThread}.
     */
    volatile boolean onComputerQueue = false;

    /**
     * The command that {@link #work()} should execute on the computer thread.
     *
     * One sets the command with {@link #queueStart()} and {@link #queueStop(boolean, boolean)}. Neither of these will
     * queue a new event if there is an existing one in the queue.
     *
     * Note, if command is not {@code null}, then some command is scheduled to be executed. Otherwise it is not
     * currently in the queue (or is currently being executed).
     */
    private volatile StateCommand command;

    /**
     * The queue of events which should be executed when this computer is on.
     *
     * Note, this should be empty if this computer is off - it is cleared on shutdown and when turning on again.
     */
    private final Queue<Event> eventQueue = new ArrayDeque<>();

    /**
     * Whether this executor has been closed, and will no longer accept any incoming commands or events.
     *
     * @see #queueStop(boolean, boolean)
     */
    private boolean closed;

    private IWritableMount rootMount;

    /**
     * {@code true} when inside {@link #work()}. We use this to ensure we're only doing one bit of work at one time.
     */
    private final AtomicBoolean isExecuting = new AtomicBoolean( false );

    ComputerExecutor( Computer computer )
    {
        // Ensure the computer thread is running as required.
        ComputerThread.start();

        this.computer = computer;

        Environment environment = computer.getEnvironment();

        // Add all default APIs to the loaded list.
        apis.add( new TermAPI( environment ) );
        apis.add( new RedstoneAPI( environment ) );
        apis.add( new FSAPI( environment ) );
        apis.add( new PeripheralAPI( environment ) );
        apis.add( new OSAPI( environment ) );
        if( ComputerCraft.http_enable ) apis.add( new HTTPAPI( environment ) );

        // Load in the externally registered APIs.
        for( ILuaAPIFactory factory : ApiFactories.getAll() )
        {
            ComputerSystem system = new ComputerSystem( environment );
            ILuaAPI api = factory.create( system );
            if( api != null ) apis.add( new ApiWrapper( api, system ) );
        }
    }

    boolean isOn()
    {
        return isOn;
    }

    FileSystem getFileSystem()
    {
        return fileSystem;
    }

    Computer getComputer()
    {
        return computer;
    }

    void addApi( ILuaAPI api )
    {
        apis.add( api );
    }

    /**
     * Schedule this computer to be started if not already on.
     */
    void queueStart()
    {
        synchronized( queueLock )
        {
            // We should only schedule a start if we're not currently on and there's turn on.
            if( closed || isOn || this.command != null ) return;

            command = StateCommand.TURN_ON;
            enqueue();
        }
    }

    /**
     * Schedule this computer to be stopped if not already on.
     *
     * @param reboot Reboot the computer after stopping
     * @param close  Close the computer after stopping.
     * @see #closed
     */
    void queueStop( boolean reboot, boolean close )
    {
        synchronized( queueLock )
        {
            if( closed ) return;
            this.closed = close;

            StateCommand newCommand = reboot ? StateCommand.REBOOT : StateCommand.SHUTDOWN;

            // We should only schedule a stop if we're currently on and there's no shutdown pending.
            if( !isOn || command != null )
            {
                // If we're closing, set the command just in case.
                if( close ) command = newCommand;
                return;
            }

            command = newCommand;
            enqueue();
        }
    }

    /**
     * Abort this whole computer due to a timeout. This will immediately destroy the Lua machine,
     * and then schedule a shutdown.
     */
    void abort()
    {
        // TODO: We need to test this much more thoroughly.
        ILuaMachine machine = this.machine;
        if( machine != null ) machine.close();

        synchronized( queueLock )
        {
            if( closed ) return;
            command = StateCommand.ABORT;
            if( isOn ) enqueue();
        }
    }

    /**
     * Queue an event if the computer is on
     *
     * @param event The event's name
     * @param args  The event's arguments
     */
    void queueEvent( @Nonnull String event, @Nullable Object[] args )
    {
        // Events should be skipped if we're not on.
        if( !isOn ) return;

        synchronized( queueLock )
        {
            // And if we've got some command in the pipeline, then don't queue events - they'll
            // probably be disposed of anyway.
            // We also limit the number of events which can be queued.
            if( closed || command != null || eventQueue.size() >= QUEUE_LIMIT ) return;

            eventQueue.offer( new Event( event, args ) );
            enqueue();
        }
    }

    /**
     * Add this executor to the {@link ComputerThread} if not already there.
     */
    private void enqueue()
    {
        synchronized( queueLock )
        {
            if( onComputerQueue ) return;
            onComputerQueue = true;
            ComputerThread.queue( this );
        }
    }

    /**
     * Update the internals of the executor.
     */
    void tick()
    {
        if( isOn && isOnLock.tryLock() )
        {
            // This horrific structure means we don't try to update APIs while the state is being changed
            // (and so they may be running startup/shutdown).
            // We use tryLock here, as it has minimal delay, and it doesn't matter if we miss an advance at the
            // beginning or end of a computer's lifetime.
            try
            {
                if( isOn )
                {
                    // Advance our APIs.
                    for( ILuaAPI api : apis ) api.update();
                }
            }
            finally
            {
                isOnLock.unlock();
            }
        }
    }

    private IMount getRomMount()
    {
        if( romMount != null ) return romMount;

        synchronized( romMountLock )
        {
            if( romMount != null ) return romMount;
            return romMount = computer.getComputerEnvironment().createResourceMount( "computercraft", "lua/rom" );
        }
    }

    private FileSystem createFileSystem()
    {
        if( rootMount == null )
        {
            rootMount = computer.getComputerEnvironment().createSaveDirMount(
                "computer/" + computer.assignID(),
                computer.getComputerEnvironment().getComputerSpaceLimit()
            );
        }

        FileSystem filesystem = null;
        try
        {
            filesystem = new FileSystem( "hdd", rootMount );

            IMount romMount = getRomMount();
            if( romMount == null )
            {
                displayFailure( "Cannot mount rom" );
                return null;
            }

            filesystem.mount( "rom", "rom", romMount );
            return filesystem;
        }
        catch( FileSystemException e )
        {
            if( filesystem != null ) filesystem.close();
            ComputerCraft.log.error( "Cannot mount computer filesystem", e );

            displayFailure( "Cannot mount computer system" );
            return null;
        }
    }

    private ILuaMachine createLuaMachine()
    {
        // Load the bios resource
        InputStream biosStream = null;
        try
        {
            biosStream = computer.getComputerEnvironment().createResourceFile( "computercraft", "lua/bios.lua" );
        }
        catch( Exception ignored )
        {
        }

        if( biosStream == null )
        {
            displayFailure( "Error loading bios.lua" );
            return null;
        }

        // Create the lua machine
        ILuaMachine machine = new CobaltLuaMachine( computer, timeout );

        // Add the APIs
        for( ILuaAPI api : apis ) machine.addAPI( api );

        // Start the machine running the bios resource
        machine.loadBios( biosStream );
        IoUtil.closeQuietly( biosStream );

        if( machine.isFinished() )
        {
            machine.close();
            displayFailure( "Error starting bios.lua" );
            return null;
        }

        return machine;
    }

    private void turnOn() throws InterruptedException
    {
        isOnLock.lockInterruptibly();
        try
        {
            // Reset the terminal and event queue
            computer.getTerminal().reset();
            synchronized( queueLock )
            {
                eventQueue.clear();
            }

            // Init filesystem
            if( (this.fileSystem = createFileSystem()) == null )
            {
                shutdown();
                return;
            }

            // Init APIs
            for( ILuaAPI api : apis ) api.startup();

            // Init lua
            if( (this.machine = createLuaMachine()) == null )
            {
                shutdown();
                return;
            }

            // Initialisation has finished, so let's mark ourselves as on.
            isOn = true;
            computer.markChanged();
        }
        finally
        {
            isOnLock.unlock();
        }

        // Now actually start the computer, now that everything is set up.
        machine.handleEvent( null, null );
    }

    private void shutdown() throws InterruptedException
    {
        isOnLock.lockInterruptibly();
        try
        {
            isOn = false;
            synchronized( queueLock )
            {
                eventQueue.clear();
            }

            // Shutdown Lua machine
            if( machine != null )
            {
                machine.close();
                machine = null;
            }

            // Shutdown our APIs
            for( ILuaAPI api : apis ) api.shutdown();

            // Unload filesystem
            if( fileSystem != null )
            {
                fileSystem.close();
                fileSystem = null;
            }

            computer.getEnvironment().resetOutput();
            computer.markChanged();
        }
        finally
        {
            isOnLock.unlock();
        }
    }

    /**
     * Called before calling {@link #work()}, setting up any important state.
     */
    void beforeWork()
    {
        timeout.reset();
    }

    /**
     * Called after executing {@link #work()}. Adds this back to the {@link ComputerThread} if we have more work,
     * otherwise remove it.
     */
    void afterWork()
    {
        Tracking.addTaskTiming( getComputer(), timeout.nanoSinceStart() );

        synchronized( queueLock )
        {
            if( eventQueue.isEmpty() && command == null )
            {
                onComputerQueue = false;
            }
            else
            {
                ComputerThread.queue( this );
            }
        }
    }

    /**
     * The main worker function, called by {@link ComputerThread}.
     *
     * This either executes a {@link StateCommand} or attempts to run an event
     *
     * @throws InterruptedException If various locks could not be acquired.
     * @see #command
     * @see #eventQueue
     */
    void work() throws InterruptedException
    {
        if( isExecuting.getAndSet( true ) )
        {
            throw new IllegalStateException( "Multiple threads running on computer the same time" );
        }

        try
        {
            StateCommand command;
            Event event = null;
            synchronized( queueLock )
            {
                command = this.command;
                this.command = null;

                // If we've no command, pull something from the event queue instead.
                if( command == null )
                {
                    if( !isOn )
                    {
                        // We're not on and had no command, but we had work queued. This should never happen, so clear
                        // the event queue just in case.
                        eventQueue.clear();
                        return;
                    }

                    event = eventQueue.poll();
                }
            }

            if( command != null )
            {
                switch( command )
                {
                    case TURN_ON:
                        if( isOn ) return;
                        turnOn();
                        break;

                    case SHUTDOWN:

                        if( !isOn ) return;
                        computer.getTerminal().reset();
                        shutdown();
                        break;

                    case REBOOT:
                        if( !isOn ) return;
                        computer.getTerminal().reset();
                        shutdown();

                        computer.turnOn();
                        break;

                    case ABORT:
                        if( !isOn ) return;
                        displayFailure( TimeoutState.ABORT_MESSAGE );
                        shutdown();
                        break;
                }
            }
            else
            {
                machine.handleEvent( event.name, event.args );
                if( machine.isFinished() )
                {
                    displayFailure( "Error resuming bios.lua" );
                    shutdown();
                }
            }
        }
        finally
        {
            isExecuting.set( false );
        }
    }

    private void displayFailure( String message )
    {
        Terminal terminal = computer.getTerminal();
        terminal.reset();
        terminal.write( message );
        terminal.setCursorPos( 0, 1 );
        terminal.write( "ComputerCraft may be installed incorrectly" );
    }

    private enum StateCommand
    {
        TURN_ON,
        SHUTDOWN,
        REBOOT,
        ABORT,
    }

    private static class Event
    {
        final String name;
        final Object[] args;

        private Event( String name, Object[] args )
        {
            this.name = name;
            this.args = args;
        }
    }
}
