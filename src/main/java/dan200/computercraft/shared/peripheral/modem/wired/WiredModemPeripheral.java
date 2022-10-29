/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.peripheral.modem.wired;

import com.google.common.collect.ImmutableMap;
import dan200.computercraft.ComputerCraft;
import dan200.computercraft.api.filesystem.IMount;
import dan200.computercraft.api.filesystem.IWritableMount;
import dan200.computercraft.api.lua.*;
import dan200.computercraft.api.network.IPacketNetwork;
import dan200.computercraft.api.network.wired.IWiredNode;
import dan200.computercraft.api.network.wired.IWiredSender;
import dan200.computercraft.api.peripheral.IComputerAccess;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.api.peripheral.IWorkMonitor;
import dan200.computercraft.api.peripheral.NotAttachedException;
import dan200.computercraft.core.apis.PeripheralAPI;
import dan200.computercraft.core.asm.PeripheralMethod;
import dan200.computercraft.shared.peripheral.modem.ModemPeripheral;
import dan200.computercraft.shared.peripheral.modem.ModemState;
import dan200.computercraft.shared.util.LuaUtil;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

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

    @Nonnull
    @Override
    public Set<String> getAdditionalTypes()
    {
        return Collections.singleton( "peripheral_hub" );
    }

    //region Peripheral methods

    /**
     * List all remote peripherals on the wired network.
     * <p>
     * If this computer is attached to the network, it _will not_ be included in
     * this list.
     * <p>
     * :::note
     * This function only appears on wired modems. Check {@link #isWireless} returns false before calling it.
     * :::
     *
     * @param computer The calling computer.
     * @return Remote peripheral names on the network.
     */
    @LuaFunction
    public final Collection<String> getNamesRemote( IComputerAccess computer )
    {
        return getWrappers( computer ).keySet();
    }

    /**
     * Determine if a peripheral is available on this wired network.
     * <p>
     * :::note
     * This function only appears on wired modems. Check {@link #isWireless} returns false before calling it.
     * :::
     *
     * @param computer The calling computer.
     * @param name     The peripheral's name.
     * @return boolean If a peripheral is present with the given name.
     * @see PeripheralAPI#isPresent
     */
    @LuaFunction
    public final boolean isPresentRemote( IComputerAccess computer, String name )
    {
        return getWrapper( computer, name ) != null;
    }

    /**
     * Get the type of a peripheral is available on this wired network.
     * <p>
     * :::note
     * This function only appears on wired modems. Check {@link #isWireless} returns false before calling it.
     * :::
     *
     * @param computer The calling computer.
     * @param name     The peripheral's name.
     * @return The peripheral's name.
     * @cc.treturn string|nil The peripheral's type, or {@code nil} if it is not present.
     * @cc.changed 1.99 Peripherals can have multiple types - this function returns multiple values.
     * @see PeripheralAPI#getType
     */
    @LuaFunction
    public final Object[] getTypeRemote( IComputerAccess computer, String name )
    {
        RemotePeripheralWrapper wrapper = getWrapper( computer, name );
        return wrapper == null ? null : LuaUtil.consArray( wrapper.getType(), wrapper.getAdditionalTypes() );
    }

    /**
     * Check a peripheral is of a particular type.
     * <p>
     * :::note
     * This function only appears on wired modems. Check {@link #isWireless} returns false before calling it.
     * :::
     *
     * @param computer The calling computer.
     * @param name     The peripheral's name.
     * @param type     The type to check.
     * @return The peripheral's name.
     * @cc.treturn boolean|nil If a peripheral has a particular type, or {@literal nil} if it is not present.
     * @cc.since 1.99
     * @see PeripheralAPI#getType
     */
    @LuaFunction
    public final Object[] hasTypeRemote( IComputerAccess computer, String name, String type )
    {
        RemotePeripheralWrapper wrapper = getWrapper( computer, name );
        return wrapper == null ? null : new Object[] { wrapper.getType().equals( type ) || wrapper.getAdditionalTypes().contains( type ) };
    }

    /**
     * Get all available methods for the remote peripheral with the given name.
     * <p>
     * :::note
     * This function only appears on wired modems. Check {@link #isWireless} returns false before calling it.
     * :::
     *
     * @param computer The calling computer.
     * @param name     The peripheral's name.
     * @return A list of methods provided by this peripheral, or {@code nil} if it is not present.
     * @cc.treturn { string... }|nil A list of methods provided by this peripheral, or {@code nil} if it is not present.
     * @see PeripheralAPI#getMethods
     */
    @LuaFunction
    public final Object[] getMethodsRemote( IComputerAccess computer, String name )
    {
        RemotePeripheralWrapper wrapper = getWrapper( computer, name );
        if( wrapper == null ) return null;

        return new Object[] { wrapper.getMethodNames() };
    }

    /**
     * Call a method on a peripheral on this wired network.
     * <p>
     * :::note
     * This function only appears on wired modems. Check {@link #isWireless} returns false before calling it.
     * :::
     *
     * @param computer  The calling computer.
     * @param context   The Lua context we're executing in.
     * @param arguments Arguments to this computer.
     * @return The peripheral's result.
     * @throws LuaException (hidden) If the method throws an error.
     * @cc.tparam string remoteName The name of the peripheral to invoke the method on.
     * @cc.tparam string method The name of the method
     * @cc.param ...      Additional arguments to pass to the method
     * @cc.treturn string The return values of the peripheral method.
     * @see PeripheralAPI#call
     */
    @LuaFunction
    public final MethodResult callRemote( IComputerAccess computer, ILuaContext context, IArguments arguments ) throws LuaException
    {
        String remoteName = arguments.getString( 0 );
        String methodName = arguments.getString( 1 );
        RemotePeripheralWrapper wrapper = getWrapper( computer, remoteName );
        if( wrapper == null ) throw new LuaException( "No peripheral: " + remoteName );

        return wrapper.callMethod( context, methodName, arguments.drop( 2 ) );
    }

    /**
     * Returns the network name of the current computer, if the modem is on. This
     * may be used by other computers on the network to wrap this computer as a
     * peripheral.
     * <p>
     * :::note
     * This function only appears on wired modems. Check {@link #isWireless} returns false before calling it.
     * :::
     *
     * @return The current computer's name.
     * @cc.treturn string|nil The current computer's name on the wired network.
     * @cc.since 1.80pr1.7
     */
    @LuaFunction
    public final Object[] getNameLocal()
    {
        String local = getLocalPeripheral().getConnectedName();
        return local == null ? null : new Object[] { local };
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
        private final WiredModemElement element;
        private final IPeripheral peripheral;
        private final IComputerAccess computer;
        private final String name;

        private final String type;
        private final Set<String> additionalTypes;
        private final Map<String, PeripheralMethod> methodMap;

        private volatile boolean attached;
        private final Set<String> mounts = new HashSet<>();

        RemotePeripheralWrapper( WiredModemElement element, IPeripheral peripheral, IComputerAccess computer, String name )
        {
            this.element = element;
            this.peripheral = peripheral;
            this.computer = computer;
            this.name = name;

            type = Objects.requireNonNull( peripheral.getType(), "Peripheral type cannot be null" );
            additionalTypes = peripheral.getAdditionalTypes();
            methodMap = PeripheralAPI.getMethods( peripheral );
        }

        public void attach()
        {
            attached = true;
            peripheral.attach( this );
            computer.queueEvent( "peripheral", getAttachmentName() );
        }

        public void detach()
        {
            peripheral.detach( this );
            computer.queueEvent( "peripheral_detach", getAttachmentName() );
            attached = false;

            synchronized( this )
            {
                if( !mounts.isEmpty() )
                {
                    ComputerCraft.log.warn( "Peripheral {} called mount but did not call unmount for {}", peripheral, mounts );
                }

                for( String mount : mounts ) computer.unmount( mount );
                mounts.clear();
            }
        }

        public String getType()
        {
            return type;
        }

        public Set<String> getAdditionalTypes()
        {
            return additionalTypes;
        }

        public Collection<String> getMethodNames()
        {
            return methodMap.keySet();
        }

        public MethodResult callMethod( ILuaContext context, String methodName, IArguments arguments ) throws LuaException
        {
            PeripheralMethod method = methodMap.get( methodName );
            if( method == null ) throw new LuaException( "No such method " + methodName );
            return method.apply( peripheral, context, this, arguments );
        }

        // IComputerAccess implementation

        @Override
        public synchronized String mount( @Nonnull String desiredLocation, @Nonnull IMount mount )
        {
            if( !attached ) throw new NotAttachedException();
            String mounted = computer.mount( desiredLocation, mount, name );
            mounts.add( mounted );
            return mounted;
        }

        @Override
        public synchronized String mount( @Nonnull String desiredLocation, @Nonnull IMount mount, @Nonnull String driveName )
        {
            if( !attached ) throw new NotAttachedException();
            String mounted = computer.mount( desiredLocation, mount, driveName );
            mounts.add( mounted );
            return mounted;
        }

        @Override
        public synchronized String mountWritable( @Nonnull String desiredLocation, @Nonnull IWritableMount mount )
        {
            if( !attached ) throw new NotAttachedException();
            String mounted = computer.mountWritable( desiredLocation, mount, name );
            mounts.add( mounted );
            return mounted;
        }

        @Override
        public synchronized String mountWritable( @Nonnull String desiredLocation, @Nonnull IWritableMount mount, @Nonnull String driveName )
        {
            if( !attached ) throw new NotAttachedException();
            String mounted = computer.mountWritable( desiredLocation, mount, driveName );
            mounts.add( mounted );
            return mounted;
        }

        @Override
        public synchronized void unmount( String location )
        {
            if( !attached ) throw new NotAttachedException();
            computer.unmount( location );
            mounts.remove( location );
        }

        @Override
        public int getID()
        {
            if( !attached ) throw new NotAttachedException();
            return computer.getID();
        }

        @Override
        public void queueEvent( @Nonnull String event, Object... arguments )
        {
            if( !attached ) throw new NotAttachedException();
            computer.queueEvent( event, arguments );
        }

        @Nonnull
        @Override
        public IWorkMonitor getMainThreadMonitor()
        {
            if( !attached ) throw new NotAttachedException();
            return computer.getMainThreadMonitor();
        }

        @Nonnull
        @Override
        public String getAttachmentName()
        {
            if( !attached ) throw new NotAttachedException();
            return name;
        }

        @Nonnull
        @Override
        public Map<String, IPeripheral> getAvailablePeripherals()
        {
            if( !attached ) throw new NotAttachedException();
            synchronized( element.getRemotePeripherals() )
            {
                return ImmutableMap.copyOf( element.getRemotePeripherals() );
            }
        }

        @Nullable
        @Override
        public IPeripheral getAvailablePeripheral( @Nonnull String name )
        {
            if( !attached ) throw new NotAttachedException();
            synchronized( element.getRemotePeripherals() )
            {
                return element.getRemotePeripherals().get( name );
            }
        }
    }
}
