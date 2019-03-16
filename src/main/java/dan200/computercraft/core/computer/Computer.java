/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.core.computer;

import com.google.common.base.Objects;
import dan200.computercraft.api.lua.ILuaAPI;
import dan200.computercraft.api.peripheral.IPeripheral;
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
 * </ul>
 */
public class Computer
{
    private static final int START_DELAY = 50;

    // Various properties of the computer
    private int m_id;
    private String m_label = null;

    // Read-only fields about the computer
    private final IComputerEnvironment m_environment;
    private final Terminal m_terminal;
    private final ComputerExecutor executor;

    // Additional state about the computer and its environment.
    private boolean m_blinking = false;
    private final Environment internalEnvironment = new Environment( this );
    private AtomicBoolean externalOutputChanged = new AtomicBoolean();

    private boolean startRequested;
    private int m_ticksSinceStart = -1;

    public Computer( IComputerEnvironment environment, Terminal terminal, int id )
    {
        m_id = id;
        m_environment = environment;
        m_terminal = terminal;

        executor = new ComputerExecutor( this );
    }

    IComputerEnvironment getComputerEnvironment()
    {
        return m_environment;
    }

    FileSystem getFileSystem()
    {
        return executor.getFileSystem();
    }

    Terminal getTerminal()
    {
        return m_terminal;
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

    public int getID()
    {
        return m_id;
    }

    public int assignID()
    {
        if( m_id < 0 )
        {
            m_id = m_environment.assignNewID();
        }
        return m_id;
    }

    public void setID( int id )
    {
        m_id = id;
    }

    public String getLabel()
    {
        return m_label;
    }

    public void setLabel( String label )
    {
        if( !Objects.equal( label, m_label ) )
        {
            m_label = label;
            externalOutputChanged.set( true );
        }
    }

    public void tick()
    {
        // We keep track of the number of ticks since the last start, only
        if( m_ticksSinceStart >= 0 && m_ticksSinceStart <= START_DELAY ) m_ticksSinceStart++;

        if( startRequested && (m_ticksSinceStart < 0 || m_ticksSinceStart > START_DELAY) )
        {
            startRequested = false;
            if( !executor.isOn() )
            {
                m_ticksSinceStart = 0;
                executor.queueStart();
            }
        }

        executor.tick();

        // Update the environment's internal state.
        internalEnvironment.update();

        // Propagate the environment's output to the world.
        if( internalEnvironment.updateOutput() ) externalOutputChanged.set( true );

        // Set output changed if the terminal has changed from blinking to not
        boolean blinking = m_terminal.getCursorBlink() &&
            m_terminal.getCursorX() >= 0 && m_terminal.getCursorX() < m_terminal.getWidth() &&
            m_terminal.getCursorY() >= 0 && m_terminal.getCursorY() < m_terminal.getHeight();
        if( blinking != m_blinking )
        {
            m_blinking = blinking;
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
        return isOn() && m_blinking;
    }

    public void addApi( ILuaAPI api )
    {
        executor.addApi( api );
    }

    @Deprecated
    public IPeripheral getPeripheral( int side )
    {
        return internalEnvironment.getPeripheral( side );
    }

    @Deprecated
    public void setPeripheral( int side, IPeripheral peripheral )
    {
        internalEnvironment.setPeripheral( side, peripheral );
    }

    @Deprecated
    public void addAPI( dan200.computercraft.core.apis.ILuaAPI api )
    {
        addApi( api );
    }

    @Deprecated
    @SuppressWarnings( "unused" )
    public void advance( double dt )
    {
        tick();
    }

    @Deprecated
    public static final String[] s_sideNames = IAPIEnvironment.SIDE_NAMES;
}
