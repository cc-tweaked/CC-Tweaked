/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.peripheral.modem.wired;

import com.google.common.collect.ImmutableMap;
import dan200.computercraft.api.filesystem.IMount;
import dan200.computercraft.api.filesystem.IWritableMount;
import dan200.computercraft.api.lua.ILuaContext;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.network.IPacketNetwork;
import dan200.computercraft.api.network.wired.IWiredNode;
import dan200.computercraft.api.network.wired.IWiredSender;
import dan200.computercraft.api.peripheral.IComputerAccess;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.api.peripheral.IWorkMonitor;
import dan200.computercraft.shared.peripheral.modem.ModemPeripheral;
import dan200.computercraft.shared.peripheral.modem.ModemState;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static dan200.computercraft.api.lua.ArgumentHelper.getString;

public abstract class WiredModemPeripheral extends ModemPeripheral implements IWiredSender
{
    private final WiredModemElement modem;

    private final Map<IComputerAccess, ConcurrentMap<String, RemotePeripheralWrapper>> peripheralWrappers = new HashMap<>( 1 );

    public WiredModemPeripheral( ModemState state, WiredModemElement modem )
    {
        super( state );
        this.modem = modem;
    }

    //region IPacketSender implementation
    @Override
    public boolean isInterdimensional()
    {
        return false;
    }

    @Override
    public double getRange()
    {
        return 256.0;
    }

    @Override
    protected IPacketNetwork getNetwork()
    {
        return modem.getNode();
    }

    @Nonnull
    @Override
    public World getWorld()
    {
        return modem.getWorld();
    }

    @Nonnull
    protected abstract WiredModemLocalPeripheral getLocalPeripheral();
    //endregion

    //region IPeripheral
    @Nonnull
    @Override
    public String[] getMethodNames()
    {
        String[] methods = super.getMethodNames();
        String[] newMethods = new String[methods.length + 6];
        System.arraycopy( methods, 0, newMethods, 0, methods.length );
        newMethods[methods.length] = "getNamesRemote";
        newMethods[methods.length + 1] = "isPresentRemote";
        newMethods[methods.length + 2] = "getTypeRemote";
        newMethods[methods.length + 3] = "getMethodsRemote";
        newMethods[methods.length + 4] = "callRemote";
        newMethods[methods.length + 5] = "getNameLocal";
        return newMethods;
    }

    @Override
    public Object[] callMethod( @Nonnull IComputerAccess computer, @Nonnull ILuaContext context, int method, @Nonnull Object[] arguments ) throws LuaException, InterruptedException
    {
        String[] methods = super.getMethodNames();
        switch( method - methods.length )
        {
            case 0:
            {
                // getNamesRemote
                Map<String, RemotePeripheralWrapper> wrappers = getWrappers( computer );
                Map<Object, Object> table = new HashMap<>();
                if( wrappers != null )
                {
                    int idx = 1;
                    for( String name : wrappers.keySet() ) table.put( idx++, name );
                }
                return new Object[] { table };
            }
            case 1:
            {
                // isPresentRemote
                String name = getString( arguments, 0 );
                return new Object[] { getWrapper( computer, name ) != null };
            }
            case 2:
            {
                // getTypeRemote
                String name = getString( arguments, 0 );
                RemotePeripheralWrapper wrapper = getWrapper( computer, name );
                return wrapper != null ? new Object[] { wrapper.getType() } : null;
            }
            case 3:
            {
                // getMethodsRemote
                String name = getString( arguments, 0 );
                RemotePeripheralWrapper wrapper = getWrapper( computer, name );
                if( wrapper == null ) return null;

                String[] methodNames = wrapper.getMethodNames();
                Map<Object, Object> table = new HashMap<>();
                for( int i = 0; i < methodNames.length; i++ )
                {
                    table.put( i + 1, methodNames[i] );
                }
                return new Object[] { table };
            }
            case 4:
            {
                // callRemote
                String remoteName = getString( arguments, 0 );
                String methodName = getString( arguments, 1 );
                RemotePeripheralWrapper wrapper = getWrapper( computer, remoteName );
                if( wrapper == null ) throw new LuaException( "No peripheral: " + remoteName );

                Object[] methodArgs = new Object[arguments.length - 2];
                System.arraycopy( arguments, 2, methodArgs, 0, arguments.length - 2 );
                return wrapper.callMethod( context, methodName, methodArgs );
            }
            case 5:
            {
                // getNameLocal
                String local = getLocalPeripheral().getConnectedName();
                return local == null ? null : new Object[] { local };
            }
            default:
            {
                // The regular modem methods
                return super.callMethod( computer, context, method, arguments );
            }
        }
    }

    @Override
    public void attach( @Nonnull IComputerAccess computer )
    {
        super.attach( computer );

        ConcurrentMap<String, RemotePeripheralWrapper> wrappers;
        synchronized( peripheralWrappers )
        {
            wrappers = peripheralWrappers.get( computer );
            if( wrappers == null ) peripheralWrappers.put( computer, wrappers = new ConcurrentHashMap<>() );
        }

        synchronized( modem.getRemotePeripherals() )
        {
            for( Map.Entry<String, IPeripheral> entry : modem.getRemotePeripherals().entrySet() )
            {
                attachPeripheralImpl( computer, wrappers, entry.getKey(), entry.getValue() );
            }
        }
    }

    @Override
    public void detach( @Nonnull IComputerAccess computer )
    {
        Map<String, RemotePeripheralWrapper> wrappers;
        synchronized( peripheralWrappers )
        {
            wrappers = peripheralWrappers.remove( computer );
        }
        if( wrappers != null )
        {
            for( RemotePeripheralWrapper wrapper : wrappers.values() ) wrapper.detach();
            wrappers.clear();
        }

        super.detach( computer );
    }

    @Override
    public boolean equals( IPeripheral other )
    {
        if( other instanceof WiredModemPeripheral )
        {
            WiredModemPeripheral otherModem = (WiredModemPeripheral) other;
            return otherModem.modem == modem;
        }
        return false;
    }
    //endregion

    @Nonnull
    @Override
    public IWiredNode getNode()
    {
        return modem.getNode();
    }

    public void attachPeripheral( String name, IPeripheral peripheral )
    {
        synchronized( peripheralWrappers )
        {
            for( Map.Entry<IComputerAccess, ConcurrentMap<String, RemotePeripheralWrapper>> entry : peripheralWrappers.entrySet() )
            {
                attachPeripheralImpl( entry.getKey(), entry.getValue(), name, peripheral );
            }
        }
    }

    public void detachPeripheral( String name )
    {
        synchronized( peripheralWrappers )
        {
            for( ConcurrentMap<String, RemotePeripheralWrapper> wrappers : peripheralWrappers.values() )
            {
                RemotePeripheralWrapper wrapper = wrappers.remove( name );
                if( wrapper != null ) wrapper.detach();
            }

        }
    }

    private void attachPeripheralImpl( IComputerAccess computer, ConcurrentMap<String, RemotePeripheralWrapper> peripherals, String periphName, IPeripheral peripheral )
    {
        if( !peripherals.containsKey( periphName ) && !periphName.equals( getLocalPeripheral().getConnectedName() ) )
        {
            RemotePeripheralWrapper wrapper = new RemotePeripheralWrapper( modem, peripheral, computer, periphName );
            peripherals.put( periphName, wrapper );
            wrapper.attach();
        }
    }

    private ConcurrentMap<String, RemotePeripheralWrapper> getWrappers( IComputerAccess computer )
    {
        synchronized( peripheralWrappers )
        {
            return peripheralWrappers.get( computer );
        }
    }

    private RemotePeripheralWrapper getWrapper( IComputerAccess computer, String remoteName )
    {
        ConcurrentMap<String, RemotePeripheralWrapper> wrappers = getWrappers( computer );
        return wrappers == null ? null : wrappers.get( remoteName );
    }

    private static class RemotePeripheralWrapper implements IComputerAccess
    {
        private final WiredModemElement m_element;
        private final IPeripheral m_peripheral;
        private final IComputerAccess m_computer;
        private final String m_name;

        private final String m_type;
        private final String[] m_methods;
        private final Map<String, Integer> m_methodMap;

        RemotePeripheralWrapper( WiredModemElement element, IPeripheral peripheral, IComputerAccess computer, String name )
        {
            m_element = element;
            m_peripheral = peripheral;
            m_computer = computer;
            m_name = name;

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

        public void attach()
        {
            m_peripheral.attach( this );
            m_computer.queueEvent( "peripheral", new Object[] { getAttachmentName() } );
        }

        public void detach()
        {
            m_peripheral.detach( this );
            m_computer.queueEvent( "peripheral_detach", new Object[] { getAttachmentName() } );
        }

        public String getType()
        {
            return m_type;
        }

        public String[] getMethodNames()
        {
            return m_methods;
        }

        public Object[] callMethod( ILuaContext context, String methodName, Object[] arguments ) throws LuaException, InterruptedException
        {
            if( m_methodMap.containsKey( methodName ) )
            {
                int method = m_methodMap.get( methodName );
                return m_peripheral.callMethod( this, context, method, arguments );
            }
            throw new LuaException( "No such method " + methodName );
        }

        // IComputerAccess implementation

        @Override
        public String mount( @Nonnull String desiredLocation, @Nonnull IMount mount )
        {
            return m_computer.mount( desiredLocation, mount, m_name );
        }

        @Override
        public String mount( @Nonnull String desiredLocation, @Nonnull IMount mount, @Nonnull String driveName )
        {
            return m_computer.mount( desiredLocation, mount, driveName );
        }

        @Override
        public String mountWritable( @Nonnull String desiredLocation, @Nonnull IWritableMount mount )
        {
            return m_computer.mountWritable( desiredLocation, mount, m_name );
        }

        @Override
        public String mountWritable( @Nonnull String desiredLocation, @Nonnull IWritableMount mount, @Nonnull String driveName )
        {
            return m_computer.mountWritable( desiredLocation, mount, driveName );
        }

        @Override
        public void unmount( String location )
        {
            m_computer.unmount( location );
        }

        @Override
        public int getID()
        {
            return m_computer.getID();
        }

        @Override
        public void queueEvent( @Nonnull String event, Object[] arguments )
        {
            m_computer.queueEvent( event, arguments );
        }

        @Nonnull
        @Override
        public IWorkMonitor getMainThreadMonitor()
        {
            return m_computer.getMainThreadMonitor();
        }

        @Nonnull
        @Override
        public String getAttachmentName()
        {
            return m_name;
        }

        @Nonnull
        @Override
        public Map<String, IPeripheral> getAvailablePeripherals()
        {
            synchronized( m_element.getRemotePeripherals() )
            {
                return ImmutableMap.copyOf( m_element.getRemotePeripherals() );
            }
        }

        @Nullable
        @Override
        public IPeripheral getAvailablePeripheral( @Nonnull String name )
        {
            synchronized( m_element.getRemotePeripherals() )
            {
                return m_element.getRemotePeripherals().get( name );
            }
        }
    }
}
