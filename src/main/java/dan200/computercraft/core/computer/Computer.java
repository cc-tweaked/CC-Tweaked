/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2017. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.core.computer;

import com.google.common.base.Objects;
import dan200.computercraft.ComputerCraft;
import dan200.computercraft.api.filesystem.IFileSystem;
import dan200.computercraft.api.filesystem.IMount;
import dan200.computercraft.api.filesystem.IWritableMount;
import dan200.computercraft.api.lua.ILuaAPI;
import dan200.computercraft.api.lua.*;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.core.apis.*;
import dan200.computercraft.core.filesystem.FileSystem;
import dan200.computercraft.core.filesystem.FileSystemException;
import dan200.computercraft.core.lua.CobaltLuaMachine;
import dan200.computercraft.core.lua.ILuaMachine;
import dan200.computercraft.core.terminal.Terminal;
import dan200.computercraft.core.tracking.Tracking;
import dan200.computercraft.core.tracking.TrackingField;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class Computer
{
    public static final String[] s_sideNames = new String[] {
        "bottom", "top", "back", "front", "right", "left",
    };

    private enum State
    {
        Off,
        Starting,
        Running,
        Stopping,
    }

    private static class APIEnvironment implements IAPIEnvironment
    {
        private Computer m_computer;
        private IAPIEnvironment.IPeripheralChangeListener m_peripheralListener;

        public APIEnvironment( Computer computer )
        {
            m_computer = computer;
            m_peripheralListener = null;
        }

        @Override
        public Computer getComputer()
        {
            return m_computer;
        }

        @Override
        public int getComputerID()
        {
            return m_computer.assignID();
        }

        @Override
        public IComputerEnvironment getComputerEnvironment()
        {
            return m_computer.m_environment;
        }

        @Override
        public Terminal getTerminal()
        {
            return m_computer.m_terminal;
        }

        @Override
        public FileSystem getFileSystem()
        {
            return m_computer.m_fileSystem;
        }

        @Override
        public void shutdown()
        {
            m_computer.shutdown();
        }

        @Override
        public void reboot()
        {
            m_computer.reboot();
        }

        @Override
        public void queueEvent( String event, Object[] args )
        {
            m_computer.queueEvent( event, args );
        }

        @Override
        public void setOutput( int side, int output )
        {
            m_computer.setRedstoneOutput( side, output );
        }

        @Override
        public int getOutput( int side )
        {
            return m_computer.getInternalRedstoneOutput( side );
        }

        @Override
        public int getInput( int side )
        {
            return m_computer.getRedstoneInput( side );
        }

        @Override
        public void setBundledOutput( int side, int output )
        {
            m_computer.setBundledRedstoneOutput( side, output );
        }

        @Override
        public int getBundledOutput( int side )
        {
            return m_computer.getInternalBundledRedstoneOutput( side );
        }

        @Override
        public int getBundledInput( int side )
        {
            return m_computer.getBundledRedstoneInput( side );
        }

        @Override
        public IPeripheral getPeripheral( int side )
        {
            synchronized( m_computer.m_peripherals )
            {
                return m_computer.m_peripherals[side];
            }
        }

        @Override
        public void setPeripheralChangeListener( IPeripheralChangeListener listener )
        {
            synchronized( m_computer.m_peripherals )
            {
                m_peripheralListener = listener;
            }
        }

        @Override
        public String getLabel()
        {
            return m_computer.getLabel();
        }

        @Override
        public void setLabel( String label )
        {
            m_computer.setLabel( label );
        }

        @Override
        public void addTrackingChange( TrackingField field, long change )
        {
            Tracking.addValue( m_computer, field, change );
        }

        public void onPeripheralChanged( int side, IPeripheral peripheral )
        {
            synchronized( m_computer.m_peripherals )
            {
                if( m_peripheralListener != null )
                {
                    m_peripheralListener.onPeripheralChanged( side, peripheral );
                }
            }
        }
    }

    private static class ComputerSystem extends ComputerAccess implements IComputerSystem
    {
        private final IAPIEnvironment m_environment;

        private ComputerSystem( IAPIEnvironment m_environment )
        {
            super( m_environment );
            this.m_environment = m_environment;
        }

        @Nonnull
        @Override
        public String getAttachmentName()
        {
            return "computer";
        }

        @Nullable
        @Override
        public IFileSystem getFileSystem()
        {
            FileSystem fs = m_environment.getFileSystem();
            return fs == null ? null : fs.getMountWrapper();
        }

        @Nullable
        @Override
        public String getLabel()
        {
            return m_environment.getLabel();
        }
    }

    private static class APIWrapper implements ILuaAPI
    {
        private final ILuaAPI delegate;
        private final ComputerSystem system;

        private APIWrapper( ILuaAPI delegate, ComputerSystem system )
        {
            this.delegate = delegate;
            this.system = system;
        }

        @Override
        public String[] getNames()
        {
            return delegate.getNames();
        }

        @Override
        public void startup()
        {
            delegate.startup();
        }

        @Override
        public void update()
        {
            delegate.update();
        }

        @Override
        public void shutdown()
        {
            delegate.shutdown();
            system.unmountAll();
        }

        @Nonnull
        @Override
        public String[] getMethodNames()
        {
            return delegate.getMethodNames();
        }

        @Nullable
        @Override
        public Object[] callMethod( @Nonnull ILuaContext context, int method, @Nonnull Object[] arguments ) throws LuaException, InterruptedException
        {
            return delegate.callMethod( context, method, arguments );
        }
    }

    private static IMount s_romMount = null;

    private int m_id;
    private String m_label;
    private final IComputerEnvironment m_environment;

    private int m_ticksSinceStart;
    private boolean m_startRequested;
    private State m_state;
    private boolean m_blinking;

    private ILuaMachine m_machine;
    private final List<ILuaAPI> m_apis;
    private final APIEnvironment m_apiEnvironment;

    private final Terminal m_terminal;
    private FileSystem m_fileSystem;
    private IWritableMount m_rootMount;

    private final int[] m_internalOutput;
    private final int[] m_internalBundledOutput;
    private boolean m_internalOutputChanged;

    private final int[] m_externalOutput;
    private final int[] m_externalBundledOutput;
    private boolean m_externalOutputChanged;

    private final int[] m_input;
    private final int[] m_bundledInput;
    private boolean m_inputChanged;

    private final IPeripheral[] m_peripherals;

    public Computer( IComputerEnvironment environment, Terminal terminal, int id )
    {
        ComputerThread.start();

        m_id = id;
        m_label = null;
        m_environment = environment;

        m_ticksSinceStart = -1;
        m_startRequested = false;
        m_state = State.Off;
        m_blinking = false;

        m_terminal = terminal;
        m_fileSystem = null;

        m_machine = null;
        m_apis = new ArrayList<>();
        m_apiEnvironment = new APIEnvironment( this );

        m_internalOutput = new int[6];
        m_internalBundledOutput = new int[6];
        m_internalOutputChanged = true;

        m_externalOutput = new int[6];
        m_externalBundledOutput = new int[6];
        m_externalOutputChanged = true;

        m_input = new int[6];
        m_bundledInput = new int[6];
        m_inputChanged = false;

        m_peripherals = new IPeripheral[6];
        for( int i = 0; i < 6; i++ )
        {
            m_peripherals[i] = null;
        }

        m_rootMount = null;
        createAPIs();
    }

    public IAPIEnvironment getAPIEnvironment()
    {
        return m_apiEnvironment;
    }

    public void turnOn()
    {
        if( m_state == State.Off )
        {
            m_startRequested = true;
        }
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
        synchronized( this )
        {
            stopComputer( false );
        }
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

    public void advance( double _dt )
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
                // Fire the redstone event if our redstone input has changed
                synchronized( m_input )
                {
                    if( m_inputChanged )
                    {
                        queueEvent( "redstone", null );
                        m_inputChanged = false;
                    }
                }

                // Advance our APIs
                synchronized( m_apis )
                {
                    for( ILuaAPI api : m_apis )
                    {
                        api.update();
                    }
                }
            }
        }

        // Set outputchanged if the internal redstone has changed
        synchronized( m_internalOutput )
        {
            if( m_internalOutputChanged )
            {
                boolean changed = false;
                for( int i = 0; i < 6; i++ )
                {
                    if( m_externalOutput[i] != m_internalOutput[i] )
                    {
                        m_externalOutput[i] = m_internalOutput[i];
                        changed = true;
                    }
                    if( m_externalBundledOutput[i] != m_internalBundledOutput[i] )
                    {
                        m_externalBundledOutput[i] = m_internalBundledOutput[i];
                        changed = true;
                    }
                }
                m_internalOutputChanged = false;
                if( changed )
                {
                    m_externalOutputChanged = true;
                }
            }
        }

        // Set outputchanged if the terminal has changed from blinking to not
        synchronized( m_terminal )
        {
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
        synchronized( m_terminal )
        {
            return isOn() && m_blinking;
        }
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
            if( s_romMount == null )
            {
                s_romMount = m_environment.createResourceMount( "computercraft", "lua/rom" );
            }
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

    // Redstone

    public int getRedstoneOutput( int side )
    {
        synchronized( m_internalOutput )
        {
            return isOn() ? m_externalOutput[side] : 0;
        }
    }

    private int getInternalRedstoneOutput( int side )
    {
        synchronized( m_internalOutput )
        {
            return isOn() ? m_internalOutput[side] : 0;
        }
    }

    private void setRedstoneOutput( int side, int level )
    {
        synchronized( m_internalOutput )
        {
            if( m_internalOutput[side] != level )
            {
                m_internalOutput[side] = level;
                m_internalOutputChanged = true;
            }
        }
    }

    public void setRedstoneInput( int side, int level )
    {
        synchronized( m_input )
        {
            if( m_input[side] != level )
            {
                m_input[side] = level;
                m_inputChanged = true;
            }
        }
    }

    private int getRedstoneInput( int side )
    {
        synchronized( m_input )
        {
            return m_input[side];
        }
    }

    public int getBundledRedstoneOutput( int side )
    {
        synchronized( m_internalOutput )
        {
            return isOn() ? m_externalBundledOutput[side] : 0;
        }
    }

    private int getInternalBundledRedstoneOutput( int side )
    {
        synchronized( m_internalOutput )
        {
            return isOn() ? m_internalBundledOutput[side] : 0;
        }
    }

    private void setBundledRedstoneOutput( int side, int combination )
    {
        synchronized( m_internalOutput )
        {
            if( m_internalBundledOutput[side] != combination )
            {
                m_internalBundledOutput[side] = combination;
                m_internalOutputChanged = true;
            }
        }
    }

    public void setBundledRedstoneInput( int side, int combination )
    {
        synchronized( m_input )
        {
            if( m_bundledInput[side] != combination )
            {
                m_bundledInput[side] = combination;
                m_inputChanged = true;
            }
        }
    }

    private int getBundledRedstoneInput( int side )
    {
        synchronized( m_input )
        {
            return m_bundledInput[side];
        }
    }

    // Peripherals

    public void addAPI( ILuaAPI api )
    {
        m_apis.add( api );
    }

    @SuppressWarnings( "deprecation" )
    public void addAPI( dan200.computercraft.core.apis.ILuaAPI api )
    {
        addAPI( (ILuaAPI) api );
    }

    public void setPeripheral( int side, IPeripheral peripheral )
    {
        synchronized( m_peripherals )
        {
            IPeripheral existing = m_peripherals[side];
            if( (existing == null && peripheral != null) ||
                (existing != null && peripheral == null) ||
                (existing != null && !existing.equals( peripheral )) )
            {
                m_peripherals[side] = peripheral;
                m_apiEnvironment.onPeripheralChanged( side, peripheral );
            }
        }
    }

    public IPeripheral getPeripheral( int side )
    {
        synchronized( m_peripherals )
        {
            return m_peripherals[side];
        }
    }

    // Lua

    private void createAPIs()
    {
        m_apis.add( new TermAPI( m_apiEnvironment ) );
        m_apis.add( new RedstoneAPI( m_apiEnvironment ) );
        m_apis.add( new FSAPI( m_apiEnvironment ) );
        m_apis.add( new PeripheralAPI( m_apiEnvironment ) );
        m_apis.add( new OSAPI( m_apiEnvironment ) );
        //m_apis.add( new BufferAPI( m_apiEnvironment ) );
        if( ComputerCraft.http_enable )
        {
            m_apis.add( new HTTPAPI( m_apiEnvironment ) );
        }

        for( ILuaAPIFactory factory : ApiFactories.getAll() )
        {
            ComputerSystem system = new ComputerSystem( m_apiEnvironment );
            ILuaAPI api = factory.create( system );
            if( api != null ) m_apis.add( api );
        }
    }

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
                    synchronized( m_terminal )
                    {
                        m_terminal.reset();
                    }

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
                        synchronized( m_terminal )
                        {
                            m_terminal.reset();
                        }

                        synchronized( m_machine )
                        {
                            m_machine.unload();
                            m_machine = null;
                        }
                    }

                    // Reset redstone output
                    synchronized( m_internalOutput )
                    {
                        for( int i = 0; i < 6; i++ )
                        {
                            m_internalOutput[i] = 0;
                            m_internalBundledOutput[i] = 0;
                        }
                        m_internalOutputChanged = true;
                    }

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
}
