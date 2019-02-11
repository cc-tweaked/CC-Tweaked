/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.core.computer;

import com.google.common.base.Objects;
import dan200.computercraft.ComputerCraft;
import dan200.computercraft.api.filesystem.IMount;
import dan200.computercraft.api.filesystem.IWritableMount;
import dan200.computercraft.api.lua.ILuaAPI;
import dan200.computercraft.api.lua.ILuaAPIFactory;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.core.apis.*;
import dan200.computercraft.core.filesystem.FileSystem;
import dan200.computercraft.core.filesystem.FileSystemException;
import dan200.computercraft.core.lua.CobaltLuaMachine;
import dan200.computercraft.core.lua.ILuaMachine;
import dan200.computercraft.core.terminal.Terminal;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class Computer
{
    private enum State
    {
        Off,
        Starting,
        Running,
        Stopping,
    }

    private static IMount s_romMount = null;

    private int m_id;
    private String m_label = null;
    private final IComputerEnvironment m_environment;

    private int m_ticksSinceStart = -1;
    private boolean m_startRequested = false;
    private State m_state = State.Off;
    private boolean m_blinking = false;

    private ILuaMachine m_machine = null;
    private final List<ILuaAPI> m_apis = new ArrayList<>();
    private final Environment m_internalEnvironment = new Environment( this );

    private final Terminal m_terminal;
    private FileSystem m_fileSystem = null;
    private IWritableMount m_rootMount = null;

    private boolean m_externalOutputChanged;

    public Computer( IComputerEnvironment environment, Terminal terminal, int id )
    {
        m_id = id;
        m_environment = environment;
        m_terminal = terminal;

        // Ensure the computer thread is running as required.
        ComputerThread.start();

        // Add all default APIs to the loaded list.
        m_apis.add( new TermAPI( m_internalEnvironment ) );
        m_apis.add( new RedstoneAPI( m_internalEnvironment ) );
        m_apis.add( new FSAPI( m_internalEnvironment ) );
        m_apis.add( new PeripheralAPI( m_internalEnvironment ) );
        m_apis.add( new OSAPI( m_internalEnvironment ) );
        if( ComputerCraft.http_enable ) m_apis.add( new HTTPAPI( m_internalEnvironment ) );

        // Load in the API registered APIs.
        for( ILuaAPIFactory factory : ApiFactories.getAll() )
        {
            ComputerSystem system = new ComputerSystem( m_internalEnvironment );
            ILuaAPI api = factory.create( system );
            if( api != null ) m_apis.add( new ApiWrapper( api, system ) );
        }
    }

    IComputerEnvironment getComputerEnvironment()
    {
        return m_environment;
    }

    FileSystem getFileSystem()
    {
        return m_fileSystem;
    }

    Terminal getTerminal()
    {
        return m_terminal;
    }

    public Environment getEnvironment()
    {
        return m_internalEnvironment;
    }

    public IAPIEnvironment getAPIEnvironment()
    {
        return m_internalEnvironment;
    }

    public void turnOn()
    {
        if( m_state == State.Off ) m_startRequested = true;
    }

    public void shutdown()
    {
        stopComputer( false );
    }

    public void reboot()
    {
        stopComputer( true );
    }

    public boolean isOn()
    {
        synchronized( this )
        {
            return m_state == State.Running;
        }
    }

    public void abort( boolean hard )
    {
        synchronized( this )
        {
            if( m_state != State.Off && m_machine != null )
            {
                if( hard )
                {
                    m_machine.hardAbort( "Too long without yielding" );
                }
                else
                {
                    m_machine.softAbort( "Too long without yielding" );
                }
            }
        }
    }

    public void unload()
    {
        stopComputer( false );
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
            m_externalOutputChanged = true;
        }
    }

    public void advance()
    {
        synchronized( this )
        {
            // Start after a number of ticks
            if( m_ticksSinceStart >= 0 )
            {
                m_ticksSinceStart++;
            }
            if( m_startRequested && (m_ticksSinceStart < 0 || m_ticksSinceStart > 50) )
            {
                startComputer();
                m_startRequested = false;
            }

            if( m_state == State.Running )
            {
                // Update the environment's internal state.
                m_internalEnvironment.update();

                // Advance our APIs
                for( ILuaAPI api : m_apis ) api.update();
            }
        }

        // Prepare to propagate the environment's output to the world.
        if( m_internalEnvironment.updateOutput() ) m_externalOutputChanged = true;

        // Set output changed if the terminal has changed from blinking to not
        boolean blinking =
            m_terminal.getCursorBlink() &&
                m_terminal.getCursorX() >= 0 && m_terminal.getCursorX() < m_terminal.getWidth() &&
                m_terminal.getCursorY() >= 0 && m_terminal.getCursorY() < m_terminal.getHeight();

        if( blinking != m_blinking )
        {
            m_blinking = blinking;
            m_externalOutputChanged = true;
        }
    }

    public boolean pollAndResetChanged()
    {
        synchronized( this )
        {
            boolean changed = m_externalOutputChanged;
            m_externalOutputChanged = false;
            return changed;
        }
    }

    public boolean isBlinking()
    {
        return isOn() && m_blinking;
    }

    public IWritableMount getRootMount()
    {
        if( m_rootMount == null )
        {
            m_rootMount = m_environment.createSaveDirMount( "computer/" + assignID(), m_environment.getComputerSpaceLimit() );
        }
        return m_rootMount;
    }

    // FileSystem

    private boolean initFileSystem()
    {
        // Create the file system
        assignID();
        try
        {
            m_fileSystem = new FileSystem( "hdd", getRootMount() );
            if( s_romMount == null ) s_romMount = m_environment.createResourceMount( "computercraft", "lua/rom" );
            if( s_romMount != null )
            {
                m_fileSystem.mount( "rom", "rom", s_romMount );
                return true;
            }
            return false;
        }
        catch( FileSystemException e )
        {
            ComputerCraft.log.error( "Cannot mount rom", e );
            return false;
        }
    }

    // Peripherals

    public void addAPI( ILuaAPI api )
    {
        m_apis.add( api );
    }

    // Lua

    private void initLua()
    {
        // Create the lua machine
        ILuaMachine machine = new CobaltLuaMachine( this );

        // Add the APIs
        for( ILuaAPI api : m_apis )
        {
            machine.addAPI( api );
            api.startup();
        }

        // Load the bios resource
        InputStream biosStream;
        try
        {
            biosStream = m_environment.createResourceFile( "computercraft", "lua/bios.lua" );
        }
        catch( Exception e )
        {
            biosStream = null;
        }

        // Start the machine running the bios resource
        if( biosStream != null )
        {
            machine.loadBios( biosStream );
            try
            {
                biosStream.close();
            }
            catch( IOException e )
            {
                // meh
            }

            if( machine.isFinished() )
            {
                m_terminal.reset();
                m_terminal.write( "Error starting bios.lua" );
                m_terminal.setCursorPos( 0, 1 );
                m_terminal.write( "ComputerCraft may be installed incorrectly" );

                machine.unload();
                m_machine = null;
            }
            else
            {
                m_machine = machine;
            }
        }
        else
        {
            m_terminal.reset();
            m_terminal.write( "Error loading bios.lua" );
            m_terminal.setCursorPos( 0, 1 );
            m_terminal.write( "ComputerCraft may be installed incorrectly" );

            machine.unload();
            m_machine = null;
        }
    }

    private void startComputer()
    {
        synchronized( this )
        {
            if( m_state != State.Off )
            {
                return;
            }
            m_state = State.Starting;
            m_externalOutputChanged = true;
            m_ticksSinceStart = 0;
        }

        // Turn the computercraft on
        final Computer computer = this;
        ComputerThread.queueTask( new ITask()
        {
            @Override
            public Computer getOwner()
            {
                return computer;
            }

            @Override
            public void execute()
            {
                synchronized( this )
                {
                    if( m_state != State.Starting )
                    {
                        return;
                    }

                    // Init terminal
                    m_terminal.reset();

                    // Init filesystem
                    if( !initFileSystem() )
                    {
                        // Init failed, so shutdown
                        m_terminal.reset();
                        m_terminal.write( "Error mounting lua/rom" );
                        m_terminal.setCursorPos( 0, 1 );
                        m_terminal.write( "ComputerCraft may be installed incorrectly" );

                        m_state = State.Running;
                        stopComputer( false );
                        return;
                    }

                    // Init lua
                    initLua();
                    if( m_machine == null )
                    {
                        m_terminal.reset();
                        m_terminal.write( "Error loading bios.lua" );
                        m_terminal.setCursorPos( 0, 1 );
                        m_terminal.write( "ComputerCraft may be installed incorrectly" );

                        // Init failed, so shutdown
                        m_state = State.Running;
                        stopComputer( false );
                        return;
                    }

                    // Start a new state
                    m_state = State.Running;
                    m_externalOutputChanged = true;
                    synchronized( m_machine )
                    {
                        m_machine.handleEvent( null, null );
                    }
                }
            }
        }, computer );
    }

    private void stopComputer( final boolean reboot )
    {
        synchronized( this )
        {
            if( m_state != State.Running )
            {
                return;
            }
            m_state = State.Stopping;
            m_externalOutputChanged = true;
        }

        // Turn the computercraft off
        final Computer computer = this;
        ComputerThread.queueTask( new ITask()
        {
            @Override
            public Computer getOwner()
            {
                return computer;
            }

            @Override
            public void execute()
            {
                synchronized( this )
                {
                    if( m_state != State.Stopping )
                    {
                        return;
                    }

                    // Shutdown our APIs
                    synchronized( m_apis )
                    {
                        for( ILuaAPI api : m_apis )
                        {
                            api.shutdown();
                        }
                    }

                    // Shutdown terminal and filesystem
                    if( m_fileSystem != null )
                    {
                        m_fileSystem.unload();
                        m_fileSystem = null;
                    }

                    if( m_machine != null )
                    {
                        m_terminal.reset();

                        synchronized( m_machine )
                        {
                            m_machine.unload();
                            m_machine = null;
                        }
                    }

                    // Reset redstone output
                    m_internalEnvironment.resetOutput();

                    m_state = State.Off;
                    m_externalOutputChanged = true;
                    if( reboot )
                    {
                        m_startRequested = true;
                    }
                }
            }
        }, computer );
    }

    public void queueEvent( final String event, final Object[] arguments )
    {
        synchronized( this )
        {
            if( m_state != State.Running )
            {
                return;
            }
        }

        final Computer computer = this;
        ITask task = new ITask()
        {
            @Override
            public Computer getOwner()
            {
                return computer;
            }

            @Override
            public void execute()
            {
                synchronized( this )
                {
                    if( m_state != State.Running )
                    {
                        return;
                    }
                }

                synchronized( m_machine )
                {
                    m_machine.handleEvent( event, arguments );
                    if( m_machine.isFinished() )
                    {
                        m_terminal.reset();
                        m_terminal.write( "Error resuming bios.lua" );
                        m_terminal.setCursorPos( 0, 1 );
                        m_terminal.write( "ComputerCraft may be installed incorrectly" );

                        stopComputer( false );
                    }
                }
            }
        };

        ComputerThread.queueTask( task, computer );
    }

    @Deprecated
    public void setPeripheral( int side, IPeripheral peripheral )
    {
        m_internalEnvironment.setPeripheral( side, peripheral );
    }

    @Deprecated
    public void addAPI( dan200.computercraft.core.apis.ILuaAPI api )
    {
        addAPI( (ILuaAPI) api );
    }

    @Deprecated
    @SuppressWarnings( "unused" )
    public void advance( double dt )
    {
        advance();
    }
}
