/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.core.apis;

import dan200.computercraft.api.filesystem.IMount;
import dan200.computercraft.api.filesystem.IWritableMount;
import dan200.computercraft.api.lua.ILuaAPI;
import dan200.computercraft.api.lua.ILuaContext;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.core.computer.ComputerSide;
import dan200.computercraft.core.tracking.TrackingField;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static dan200.computercraft.api.lua.ArgumentHelper.getString;

public class PeripheralAPI implements ILuaAPI, IAPIEnvironment.IPeripheralChangeListener
{
    private class PeripheralWrapper extends ComputerAccess
    {
        private final String m_side;
        private final IPeripheral m_peripheral;

        private String m_type;
        private String[] m_methods;
        private Map<String, Integer> m_methodMap;
        private boolean m_attached;

        PeripheralWrapper( IPeripheral peripheral, String side )
        {
            super( m_environment );
            m_side = side;
            m_peripheral = peripheral;
            m_attached = false;

            m_type = peripheral.getType();
            m_methods = peripheral.getMethodNames();
            assert m_type != null;
            assert m_methods != null;

            m_methodMap = new HashMap<>();
            for( int i = 0; i < m_methods.length; i++ )
            {
                if( m_methods[i] != null )
                {
                    m_methodMap.put( m_methods[i], i );
                }
            }
        }

        public IPeripheral getPeripheral()
        {
            return m_peripheral;
        }

        public String getType()
        {
            return m_type;
        }

        public String[] getMethods()
        {
            return m_methods;
        }

        public synchronized boolean isAttached()
        {
            return m_attached;
        }

        public synchronized void attach()
        {
            m_attached = true;
            m_peripheral.attach( this );
        }

        public void detach()
        {
            // Call detach
            m_peripheral.detach( this );

            synchronized( this )
            {
                // Unmount everything the detach function forgot to do
                unmountAll();
            }

            m_attached = false;
        }

        public Object[] call( ILuaContext context, String methodName, Object[] arguments ) throws LuaException, InterruptedException
        {
            int method = -1;
            synchronized( this )
            {
                if( m_methodMap.containsKey( methodName ) )
                {
                    method = m_methodMap.get( methodName );
                }
            }
            if( method >= 0 )
            {
                m_environment.addTrackingChange( TrackingField.PERIPHERAL_OPS );
                return m_peripheral.callMethod( this, context, method, arguments );
            }
            else
            {
                throw new LuaException( "No such method " + methodName );
            }
        }

        // IComputerAccess implementation
        @Override
        public synchronized String mount( @Nonnull String desiredLoc, @Nonnull IMount mount, @Nonnull String driveName )
        {
            if( !m_attached )
            {
                throw new RuntimeException( "You are not attached to this Computer" );
            }

            return super.mount( desiredLoc, mount, driveName );
        }

        @Override
        public synchronized String mountWritable( @Nonnull String desiredLoc, @Nonnull IWritableMount mount, @Nonnull String driveName )
        {
            if( !m_attached )
            {
                throw new RuntimeException( "You are not attached to this Computer" );
            }

            return super.mountWritable( desiredLoc, mount, driveName );
        }

        @Override
        public synchronized void unmount( String location )
        {
            if( !m_attached )
            {
                throw new RuntimeException( "You are not attached to this Computer" );
            }

            super.unmount( location );
        }

        @Override
        public int getID()
        {
            if( !m_attached )
            {
                throw new RuntimeException( "You are not attached to this Computer" );
            }
            return super.getID();
        }

        @Override
        public void queueEvent( @Nonnull final String event, final Object[] arguments )
        {
            if( !m_attached )
            {
                throw new RuntimeException( "You are not attached to this Computer" );
            }
            super.queueEvent( event, arguments );
        }

        @Nonnull
        @Override
        public String getAttachmentName()
        {
            if( !m_attached )
            {
                throw new RuntimeException( "You are not attached to this Computer" );
            }
            return m_side;
        }

        @Nonnull
        @Override
        public Map<String, IPeripheral> getAvailablePeripherals()
        {
            if( !m_attached )
            {
                throw new RuntimeException( "You are not attached to this Computer" );
            }

            Map<String, IPeripheral> peripherals = new HashMap<>();
            for( PeripheralWrapper wrapper : m_peripherals )
            {
                if( wrapper != null && wrapper.isAttached() )
                {
                    peripherals.put( wrapper.getAttachmentName(), wrapper.getPeripheral() );
                }
            }

            return Collections.unmodifiableMap( peripherals );
        }

        @Nullable
        @Override
        public IPeripheral getAvailablePeripheral( @Nonnull String name )
        {
            if( !m_attached )
            {
                throw new RuntimeException( "You are not attached to this Computer" );
            }

            for( PeripheralWrapper wrapper : m_peripherals )
            {
                if( wrapper != null && wrapper.isAttached() && wrapper.getAttachmentName().equals( name ) )
                {
                    return wrapper.getPeripheral();
                }
            }
            return null;
        }
    }

    private final IAPIEnvironment m_environment;
    private final PeripheralWrapper[] m_peripherals;
    private boolean m_running;

    public PeripheralAPI( IAPIEnvironment environment )
    {
        m_environment = environment;
        m_environment.setPeripheralChangeListener( this );

        m_peripherals = new PeripheralWrapper[6];
        for( int i = 0; i < 6; i++ )
        {
            m_peripherals[i] = null;
        }

        m_running = false;
    }

    // IPeripheralChangeListener

    @Override
    public void onPeripheralChanged( ComputerSide side, IPeripheral newPeripheral )
    {
        synchronized( m_peripherals )
        {
            int index = side.ordinal();
            if( m_peripherals[index] != null )
            {
                // Queue a detachment
                final PeripheralWrapper wrapper = m_peripherals[index];
                if( wrapper.isAttached() ) wrapper.detach();

                // Queue a detachment event
                m_environment.queueEvent( "peripheral_detach", new Object[] { side.getName() } );
            }

            // Assign the new peripheral
            m_peripherals[index] = newPeripheral == null ? null
                : new PeripheralWrapper( newPeripheral, side.getName() );

            if( m_peripherals[index] != null )
            {
                // Queue an attachment
                final PeripheralWrapper wrapper = m_peripherals[index];
                if( m_running && !wrapper.isAttached() ) wrapper.attach();

                // Queue an attachment event
                m_environment.queueEvent( "peripheral", new Object[] { side.getName() } );
            }
        }
    }

    // ILuaAPI implementation

    @Override
    public String[] getNames()
    {
        return new String[] { "peripheral" };
    }

    @Override
    public void startup()
    {
        synchronized( m_peripherals )
        {
            m_running = true;
            for( int i = 0; i < 6; i++ )
            {
                PeripheralWrapper wrapper = m_peripherals[i];
                if( wrapper != null && !wrapper.isAttached() ) wrapper.attach();
            }
        }
    }

    @Override
    public void shutdown()
    {
        synchronized( m_peripherals )
        {
            m_running = false;
            for( int i = 0; i < 6; i++ )
            {
                PeripheralWrapper wrapper = m_peripherals[i];
                if( wrapper != null && wrapper.isAttached() )
                {
                    wrapper.detach();
                }
            }
        }
    }

    @Nonnull
    @Override
    public String[] getMethodNames()
    {
        return new String[] {
            "isPresent",
            "getType",
            "getMethods",
            "call",
        };
    }

    @Override
    public Object[] callMethod( @Nonnull ILuaContext context, int method, @Nonnull Object[] args ) throws LuaException, InterruptedException
    {
        switch( method )
        {
            case 0:
            {
                // isPresent
                boolean present = false;
                ComputerSide side = ComputerSide.valueOfInsensitive( getString( args, 0 ) );
                if( side != null )
                {
                    synchronized( m_peripherals )
                    {
                        PeripheralWrapper p = m_peripherals[side.ordinal()];
                        if( p != null ) present = true;
                    }
                }
                return new Object[] { present };
            }
            case 1:
            {
                // getType
                ComputerSide side = ComputerSide.valueOfInsensitive( getString( args, 0 ) );
                if( side != null )
                {
                    synchronized( m_peripherals )
                    {
                        PeripheralWrapper p = m_peripherals[side.ordinal()];
                        if( p != null ) return new Object[] { p.getType() };
                    }
                }
                return null;
            }
            case 2:
            {
                // getMethods
                ComputerSide side = ComputerSide.valueOfInsensitive( getString( args, 0 ) );
                if( side != null )
                {
                    synchronized( m_peripherals )
                    {
                        PeripheralWrapper p = m_peripherals[side.ordinal()];
                        if( p != null ) return new Object[] { p.getMethods() };
                    }
                }

                return null;
            }
            case 3:
            {
                // call
                ComputerSide side = ComputerSide.valueOfInsensitive( getString( args, 0 ) );
                String methodName = getString( args, 1 );
                Object[] methodArgs = Arrays.copyOfRange( args, 2, args.length );

                if( side == null ) throw new LuaException( "No peripheral attached" );

                PeripheralWrapper p;
                synchronized( m_peripherals )
                {
                    p = m_peripherals[side.ordinal()];
                }
                if( p == null ) throw new LuaException( "No peripheral attached" );

                try
                {
                    return p.call( context, methodName, methodArgs );
                }
                catch( LuaException e )
                {
                    // We increase the error level by one in order to shift the error level to where peripheral.call was
                    // invoked. It would be possible to do it in Lua code, but would add significantly more overhead.
                    if( e.getLevel() > 0 ) throw new FastLuaException( e.getMessage(), e.getLevel() + 1 );
                    throw e;
                }
            }
            default:
                return null;
        }
    }

}
