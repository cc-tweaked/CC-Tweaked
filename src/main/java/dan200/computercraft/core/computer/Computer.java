/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.core.computer;

import com.google.common.base.Objects;
import dan200.computercraft.api.lua.ILuaAPI;
import dan200.computercraft.api.peripheral.IWorkMonitor;
import dan200.computercraft.core.apis.IAPIEnvironment;
import dan200.computercraft.core.filesystem.FileSystem;
import dan200.computercraft.core.terminal.Terminal;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Represents a computer which may exist in-world or elsewhere.
 *
 * Note, this class has several (read: far, far too many) responsibilities, so can get a little unwieldy at times.
 *
 * <ul>
 * <li>Updates the {@link Environment}.</li>
 * <li>Keeps track of whether the computer is on and blinking.</li>
 * <li>Monitors whether the computer's visible state (redstone, on/off/blinking) has changed.</li>
 * <li>Passes commands and events to the {@link ComputerExecutor}.</li>
 * <li>Passes main thread tasks to the {@link MainThreadExecutor}.</li>
 * </ul>
 */
public class Computer
{
    private static final int START_DELAY = 50;

    // Various properties of the computer
    private int id;
    private String label = null;

    // Read-only fields about the computer
    private final IComputerEnvironment environment;
    private final Terminal terminal;
    private final ComputerExecutor executor;
    private final MainThreadExecutor serverExecutor;

    // Additional state about the computer and its environment.
    private boolean blinking = false;
    private final Environment internalEnvironment = new Environment( this );
    private final AtomicBoolean externalOutputChanged = new AtomicBoolean();

    private boolean startRequested;
    private int ticksSinceStart = -1;

    public Computer( IComputerEnvironment environment, Terminal terminal, int id )
    {
        this.id = id;
        this.environment = environment;
        this.terminal = terminal;

        executor = new ComputerExecutor( this );
        serverExecutor = new MainThreadExecutor( this );
    }

    IComputerEnvironment getComputerEnvironment()
    {
        return environment;
    }

    FileSystem getFileSystem()
    {
        return executor.getFileSystem();
    }

    Terminal getTerminal()
    {
        return terminal;
    }

    public Environment getEnvironment()
    {
        return internalEnvironment;
    }

    public IAPIEnvironment getAPIEnvironment()
    {
        return internalEnvironment;
    }

    public boolean isOn()
    {
        return executor.isOn();
    }

    public void turnOn()
    {
        startRequested = true;
    }

    public void shutdown()
    {
        executor.queueStop( false, false );
    }

    public void reboot()
    {
        executor.queueStop( true, false );
    }

    public void unload()
    {
        executor.queueStop( false, true );
    }

    public void queueEvent( String event, Object[] args )
    {
        executor.queueEvent( event, args );
    }

    /**
     * Queue a task to be run on the main thread, using {@link MainThread}.
     *
     * @param runnable The task to run
     * @return If the task was successfully queued (namely, whether there is space on it).
     */
    public boolean queueMainThread( Runnable runnable )
    {
        return serverExecutor.enqueue( runnable );
    }

    public IWorkMonitor getMainThreadMonitor()
    {
        return serverExecutor;
    }

    public int getID()
    {
        return id;
    }

    public int assignID()
    {
        if( id < 0 )
        {
            id = environment.assignNewID();
        }
        return id;
    }

    public void setID( int id )
    {
        this.id = id;
    }

    public String getLabel()
    {
        return label;
    }

    public void setLabel( String label )
    {
        if( !Objects.equal( label, this.label ) )
        {
            this.label = label;
            externalOutputChanged.set( true );
        }
    }

    public void tick()
    {
        // We keep track of the number of ticks since the last start, only
        if( ticksSinceStart >= 0 && ticksSinceStart <= START_DELAY ) ticksSinceStart++;

        if( startRequested && (ticksSinceStart < 0 || ticksSinceStart > START_DELAY) )
        {
            startRequested = false;
            if( !executor.isOn() )
            {
                ticksSinceStart = 0;
                executor.queueStart();
            }
        }

        executor.tick();

        // Update the environment's internal state.
        internalEnvironment.tick();

        // Propagate the environment's output to the world.
        if( internalEnvironment.updateOutput() ) externalOutputChanged.set( true );

        // Set output changed if the terminal has changed from blinking to not
        boolean blinking = terminal.getCursorBlink() &&
            terminal.getCursorX() >= 0 && terminal.getCursorX() < terminal.getWidth() &&
            terminal.getCursorY() >= 0 && terminal.getCursorY() < terminal.getHeight();
        if( blinking != this.blinking )
        {
            this.blinking = blinking;
            externalOutputChanged.set( true );
        }
    }

    void markChanged()
    {
        externalOutputChanged.set( true );
    }

    public boolean pollAndResetChanged()
    {
        return externalOutputChanged.getAndSet( false );
    }

    public boolean isBlinking()
    {
        return isOn() && blinking;
    }

    public void addApi( ILuaAPI api )
    {
        executor.addApi( api );
    }
}
