/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. Do not distribute without permission.
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
import dan200.computercraft.core.lua.MachineResult;
import dan200.computercraft.core.terminal.Terminal;
import dan200.computercraft.core.tracking.Tracking;
import dan200.computercraft.shared.util.Colour;
import dan200.computercraft.shared.util.IoUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.InputStream;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;

/**
 * The main task queue and executor for a single computer. This handles turning on and off a computer, as well as running events.
 *
 * When the computer is instructed to turn on or off, or handle an event, we queue a task and register this to be executed on the {@link ComputerThread}.
 * Note, as we may be starting many events in a single tick, the external cannot lock on anything which may be held for a long time.
 *
 * The executor is effectively composed of two separate queues. Firstly, we have a "single element" queue {@link #command} which determines which state the
 * computer should transition too. This is set by {@link #queueStart()} and {@link #queueStop(boolean, boolean)}.
 *
 * When a computer is on, we simply push any events onto to the {@link #eventQueue}.
 *
 * Both queues are run from the {@link #work()} method, which tries to execute a command if one exists, or resumes the machine with an event otherwise.
 *
 * One final responsibility for the executor is calling {@link ILuaAPI#update()} every tick, via the {@link #tick()} method. This should only be called when
 * the computer is actually on ({@link #isOn}).
 */
final class ComputerExecutor
{
    private static final int QUEUE_LIMIT = 256;
    final TimeoutState timeout = new TimeoutState();
    /**
     * The thread the executor is running on. This is non-null when performing work. We use this to ensure we're only doing one bit of work at one time.
     *
     * @see ComputerThread
     */
    final AtomicReference<Thread> executingThread = new AtomicReference<>();
    private final Computer computer;
    private final List<ILuaAPI> apis = new ArrayList<>();
    /**
     * The lock to acquire when you need to modify the "on state" of a computer.
     *
     * We hold this lock when running any command, and attempt to hold it when updating APIs. This ensures you don't update APIs while also
     * starting/stopping them.
     *
     * @see #isOn
     * @see #tick()
     * @see #turnOn()
     * @see #shutdown()
     */
    private final ReentrantLock isOnLock = new ReentrantLock();
    /**
     * A lock used for any changes to {@link #eventQueue}, {@link #command} or {@link #onComputerQueue}. This will be used on the main thread, so locks
     * should be kept as brief as possible.
     */
    private final Object queueLock = new Object();
    /**
     * The queue of events which should be executed when this computer is on.
     *
     * Note, this should be empty if this computer is off - it is cleared on shutdown and when turning on again.
     */
    private final Queue<Event> eventQueue = new ArrayDeque<>( 4 );
    /**
     * Determines if this executor is present within {@link ComputerThread}.
     *
     * @see #queueLock
     * @see #enqueue()
     * @see #afterWork()
     */
    volatile boolean onComputerQueue = false;
    /**
     * The amount of time this computer has used on a theoretical machine which shares work evenly amongst computers.
     *
     * @see ComputerThread
     */
    long virtualRuntime = 0;
    /**
     * The last time at which we updated {@link #virtualRuntime}.
     *
     * @see ComputerThread
     */
    long vRuntimeStart;
    private FileSystem fileSystem;
    private ILuaMachine machine;
    /**
     * Whether the computer is currently on. This is set to false when a shutdown starts, or when turning on completes (but just before the Lua machine is
     * started).
     *
     * @see #isOnLock
     */
    private volatile boolean isOn = false;
    /**
     * The command that {@link #work()} should execute on the computer thread.
     *
     * One sets the command with {@link #queueStart()} and {@link #queueStop(boolean, boolean)}. Neither of these will queue a new event if there is an
     * existing one in the queue.
     *
     * Note, if command is not {@code null}, then some command is scheduled to be executed. Otherwise it is not currently in the queue (or is currently
     * being executed).
     */
    private volatile StateCommand command;
    /**
     * Whether we interrupted an event and so should resume it instead of executing another task.
     *
     * @see #work()
     * @see #resumeMachine(String, Object[])
     */
    private boolean interruptedEvent = false;
    /**
     * Whether this executor has been closed, and will no longer accept any incoming commands or events.
     *
     * @see #queueStop(boolean, boolean)
     */
    private boolean closed;
    private IWritableMount rootMount;

    ComputerExecutor( Computer computer )
    {
        // Ensure the computer thread is running as required.
        ComputerThread.start();

        this.computer = computer;

        Environment environment = computer.getEnvironment();

        // Add all default APIs to the loaded list.
        this.apis.add( new TermAPI( environment ) );
        this.apis.add( new RedstoneAPI( environment ) );
        this.apis.add( new FSAPI( environment ) );
        this.apis.add( new PeripheralAPI( environment ) );
        this.apis.add( new OSAPI( environment ) );
        if( ComputerCraft.httpEnabled )
        {
            this.apis.add( new HTTPAPI( environment ) );
        }

        // Load in the externally registered APIs.
        for( ILuaAPIFactory factory : ApiFactories.getAll() )
        {
            ComputerSystem system = new ComputerSystem( environment );
            ILuaAPI api = factory.create( system );
            if( api != null )
            {
                this.apis.add( new ApiWrapper( api, system ) );
            }
        }
    }

    boolean isOn()
    {
        return this.isOn;
    }

    FileSystem getFileSystem()
    {
        return this.fileSystem;
    }

    void addApi( ILuaAPI api )
    {
        this.apis.add( api );
    }

    /**
     * Schedule this computer to be started if not already on.
     */
    void queueStart()
    {
        synchronized( this.queueLock )
        {
            // We should only schedule a start if we're not currently on and there's turn on.
            if( this.closed || this.isOn || this.command != null )
            {
                return;
            }

            this.command = StateCommand.TURN_ON;
            this.enqueue();
        }
    }

    /**
     * Add this executor to the {@link ComputerThread} if not already there.
     */
    private void enqueue()
    {
        synchronized( this.queueLock )
        {
            if( !this.onComputerQueue )
            {
                ComputerThread.queue( this );
            }
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
        synchronized( this.queueLock )
        {
            if( this.closed )
            {
                return;
            }
            this.closed = close;

            StateCommand newCommand = reboot ? StateCommand.REBOOT : StateCommand.SHUTDOWN;

            // We should only schedule a stop if we're currently on and there's no shutdown pending.
            if( !this.isOn || this.command != null )
            {
                // If we're closing, set the command just in case.
                if( close )
                {
                    this.command = newCommand;
                }
                return;
            }

            this.command = newCommand;
            this.enqueue();
        }
    }

    /**
     * Abort this whole computer due to a timeout. This will immediately destroy the Lua machine, and then schedule a shutdown.
     */
    void abort()
    {
        ILuaMachine machine = this.machine;
        if( machine != null )
        {
            machine.close();
        }

        synchronized( this.queueLock )
        {
            if( this.closed )
            {
                return;
            }
            this.command = StateCommand.ABORT;
            if( this.isOn )
            {
                this.enqueue();
            }
        }
    }

    /**
     * Queue an event if the computer is on.
     *
     * @param event The event's name
     * @param args  The event's arguments
     */
    void queueEvent( @Nonnull String event, @Nullable Object[] args )
    {
        // Events should be skipped if we're not on.
        if( !this.isOn )
        {
            return;
        }

        synchronized( this.queueLock )
        {
            // And if we've got some command in the pipeline, then don't queue events - they'll
            // probably be disposed of anyway.
            // We also limit the number of events which can be queued.
            if( this.closed || this.command != null || this.eventQueue.size() >= QUEUE_LIMIT )
            {
                return;
            }

            this.eventQueue.offer( new Event( event, args ) );
            this.enqueue();
        }
    }

    /**
     * Update the internals of the executor.
     */
    void tick()
    {
        if( this.isOn && this.isOnLock.tryLock() )
        {
            // This horrific structure means we don't try to update APIs while the state is being changed
            // (and so they may be running startup/shutdown).
            // We use tryLock here, as it has minimal delay, and it doesn't matter if we miss an advance at the
            // beginning or end of a computer's lifetime.
            try
            {
                if( this.isOn )
                {
                    // Advance our APIs.
                    for( ILuaAPI api : this.apis )
                    {
                        api.update();
                    }
                }
            }
            finally
            {
                this.isOnLock.unlock();
            }
        }
    }

    /**
     * Called before calling {@link #work()}, setting up any important state.
     */
    void beforeWork()
    {
        this.vRuntimeStart = System.nanoTime();
        this.timeout.startTimer();
    }

    /**
     * Called after executing {@link #work()}.
     *
     * @return If we have more work to do.
     */
    boolean afterWork()
    {
        if( this.interruptedEvent )
        {
            this.timeout.pauseTimer();
        }
        else
        {
            this.timeout.stopTimer();
        }

        Tracking.addTaskTiming( this.getComputer(), this.timeout.nanoCurrent() );

        if( this.interruptedEvent )
        {
            return true;
        }

        synchronized( this.queueLock )
        {
            if( this.eventQueue.isEmpty() && this.command == null )
            {
                return this.onComputerQueue = false;
            }
            return true;
        }
    }

    Computer getComputer()
    {
        return this.computer;
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
        if( this.interruptedEvent )
        {
            this.interruptedEvent = false;
            if( this.machine != null )
            {
                this.resumeMachine( null, null );
                return;
            }
        }

        StateCommand command;
        Event event = null;
        synchronized( this.queueLock )
        {
            command = this.command;
            this.command = null;

            // If we've no command, pull something from the event queue instead.
            if( command == null )
            {
                if( !this.isOn )
                {
                    // We're not on and had no command, but we had work queued. This should never happen, so clear
                    // the event queue just in case.
                    this.eventQueue.clear();
                    return;
                }

                event = this.eventQueue.poll();
            }
        }

        if( command != null )
        {
            switch( command )
            {
                case TURN_ON:
                    if( this.isOn )
                    {
                        return;
                    }
                    this.turnOn();
                    break;

                case SHUTDOWN:

                    if( !this.isOn )
                    {
                        return;
                    }
                    this.computer.getTerminal()
                        .reset();
                    this.shutdown();
                    break;

                case REBOOT:
                    if( !this.isOn )
                    {
                        return;
                    }
                    this.computer.getTerminal()
                        .reset();
                    this.shutdown();

                    this.computer.turnOn();
                    break;

                case ABORT:
                    if( !this.isOn )
                    {
                        return;
                    }
                    this.displayFailure( "Error running computer", TimeoutState.ABORT_MESSAGE );
                    this.shutdown();
                    break;
            }
        }
        else if( event != null )
        {
            this.resumeMachine( event.name, event.args );
        }
    }

    private void resumeMachine( String event, Object[] args ) throws InterruptedException
    {
        MachineResult result = this.machine.handleEvent( event, args );
        this.interruptedEvent = result.isPause();
        if( !result.isError() )
        {
            return;
        }

        this.displayFailure( "Error running computer", result.getMessage() );
        this.shutdown();
    }

    private void turnOn() throws InterruptedException
    {
        this.isOnLock.lockInterruptibly();
        try
        {
            // Reset the terminal and event queue
            this.computer.getTerminal()
                .reset();
            this.interruptedEvent = false;
            synchronized( this.queueLock )
            {
                this.eventQueue.clear();
            }

            // Init filesystem
            if( (this.fileSystem = this.createFileSystem()) == null )
            {
                this.shutdown();
                return;
            }

            // Init APIs
            this.computer.getEnvironment()
                .reset();
            for( ILuaAPI api : this.apis )
            {
                api.startup();
            }

            // Init lua
            if( (this.machine = this.createLuaMachine()) == null )
            {
                this.shutdown();
                return;
            }

            // Initialisation has finished, so let's mark ourselves as on.
            this.isOn = true;
            this.computer.markChanged();
        }
        finally
        {
            this.isOnLock.unlock();
        }

        // Now actually start the computer, now that everything is set up.
        this.resumeMachine( null, null );
    }

    private void shutdown() throws InterruptedException
    {
        this.isOnLock.lockInterruptibly();
        try
        {
            this.isOn = false;
            this.interruptedEvent = false;
            synchronized( this.queueLock )
            {
                this.eventQueue.clear();
            }

            // Shutdown Lua machine
            if( this.machine != null )
            {
                this.machine.close();
                this.machine = null;
            }

            // Shutdown our APIs
            for( ILuaAPI api : this.apis )
            {
                api.shutdown();
            }
            this.computer.getEnvironment()
                .reset();

            // Unload filesystem
            if( this.fileSystem != null )
            {
                this.fileSystem.close();
                this.fileSystem = null;
            }

            this.computer.getEnvironment()
                .resetOutput();
            this.computer.markChanged();
        }
        finally
        {
            this.isOnLock.unlock();
        }
    }

    private void displayFailure( String message, String extra )
    {
        Terminal terminal = this.computer.getTerminal();
        boolean colour = this.computer.getComputerEnvironment()
            .isColour();
        terminal.reset();

        // Display our primary error message
        if( colour )
        {
            terminal.setTextColour( 15 - Colour.RED.ordinal() );
        }
        terminal.write( message );

        if( extra != null )
        {
            // Display any additional information. This generally comes from the Lua Machine, such as compilation or
            // runtime errors.
            terminal.setCursorPos( 0, terminal.getCursorY() + 1 );
            terminal.write( extra );
        }

        // And display our generic "CC may be installed incorrectly" message.
        terminal.setCursorPos( 0, terminal.getCursorY() + 1 );
        if( colour )
        {
            terminal.setTextColour( 15 - Colour.WHITE.ordinal() );
        }
        terminal.write( "ComputerCraft may be installed incorrectly" );
    }

    private FileSystem createFileSystem()
    {
        FileSystem filesystem = null;
        try
        {
            filesystem = new FileSystem( "hdd", this.getRootMount() );

            IMount romMount = this.getRomMount();
            if( romMount == null )
            {
                this.displayFailure( "Cannot mount ROM", null );
                return null;
            }

            filesystem.mount( "rom", "rom", romMount );
            return filesystem;
        }
        catch( FileSystemException e )
        {
            if( filesystem != null )
            {
                filesystem.close();
            }
            ComputerCraft.log.error( "Cannot mount computer filesystem", e );

            this.displayFailure( "Cannot mount computer system", null );
            return null;
        }
    }

    private ILuaMachine createLuaMachine()
    {
        // Load the bios resource
        InputStream biosStream = null;
        try
        {
            biosStream = this.computer.getComputerEnvironment()
                .createResourceFile( "computercraft", "lua/bios.lua" );
        }
        catch( Exception ignored )
        {
        }

        if( biosStream == null )
        {
            this.displayFailure( "Error loading bios.lua", null );
            return null;
        }

        // Create the lua machine
        ILuaMachine machine = new CobaltLuaMachine( this.computer, this.timeout );

        // Add the APIs. We unwrap them (yes, this is horrible) to get access to the underlying object.
        for( ILuaAPI api : this.apis )
        {
            machine.addAPI( api instanceof ApiWrapper ? ((ApiWrapper) api).getDelegate() : api );
        }

        // Start the machine running the bios resource
        MachineResult result = machine.loadBios( biosStream );
        IoUtil.closeQuietly( biosStream );

        if( result.isError() )
        {
            machine.close();
            this.displayFailure( "Error loading bios.lua", result.getMessage() );
            return null;
        }

        return machine;
    }

    private IWritableMount getRootMount()
    {
        if( this.rootMount == null )
        {
            this.rootMount = this.computer.getComputerEnvironment()
                .createSaveDirMount( "computer/" + this.computer.assignID(), this.computer.getComputerEnvironment()
                    .getComputerSpaceLimit() );
        }
        return this.rootMount;
    }

    private IMount getRomMount()
    {
        return this.computer.getComputerEnvironment()
            .createResourceMount( "computercraft", "lua/rom" );
    }

    private enum StateCommand
    {
        TURN_ON, SHUTDOWN, REBOOT, ABORT,
    }

    private static final class Event
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
