/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.core.apis;

import dan200.computercraft.api.filesystem.IMount;
import dan200.computercraft.api.filesystem.IWritableMount;
import dan200.computercraft.api.lua.*;
import dan200.computercraft.api.peripheral.IDynamicPeripheral;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.api.peripheral.IWorkMonitor;
import dan200.computercraft.api.peripheral.NotAttachedException;
import dan200.computercraft.core.asm.LuaMethod;
import dan200.computercraft.core.asm.NamedMethod;
import dan200.computercraft.core.asm.PeripheralMethod;
import dan200.computercraft.core.computer.ComputerSide;
import dan200.computercraft.core.tracking.TrackingField;
import dan200.computercraft.shared.util.LuaUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

/**
 * CC's "native" peripheral API. This is wrapped within CraftOS to provide a version which works with modems.
 *
 * @cc.module peripheral
 * @hidden
 */
public class PeripheralAPI implements ILuaAPI, IAPIEnvironment.IPeripheralChangeListener
{
    private class PeripheralWrapper extends ComputerAccess
    {
        private final String side;
        private final IPeripheral peripheral;

        private final String type;
        private final Set<String> additionalTypes;
        private final Map<String, PeripheralMethod> methodMap;
        private boolean attached = false;

        PeripheralWrapper( IPeripheral peripheral, String side )
        {
            super( environment );
            this.side = side;
            this.peripheral = peripheral;

            type = Objects.requireNonNull( peripheral.getType(), "Peripheral type cannot be null" );
            additionalTypes = peripheral.getAdditionalTypes();

            methodMap = PeripheralAPI.getMethods( peripheral );
        }

        public IPeripheral getPeripheral()
        {
            return peripheral;
        }

        public String getType()
        {
            return type;
        }

        public Set<String> getAdditionalTypes()
        {
            return additionalTypes;
        }

        public Collection<String> getMethods()
        {
            return methodMap.keySet();
        }

        public synchronized boolean isAttached()
        {
            return attached;
        }

        public synchronized void attach()
        {
            attached = true;
            peripheral.attach( this );
        }

        public void detach()
        {
            // Call detach
            peripheral.detach( this );

            synchronized( this )
            {
                // Unmount everything the detach function forgot to do
                unmountAll();
            }

            attached = false;
        }

        public MethodResult call( ILuaContext context, String methodName, IArguments arguments ) throws LuaException
        {
            PeripheralMethod method;
            synchronized( this )
            {
                method = methodMap.get( methodName );
            }

            if( method == null ) throw new LuaException( "No such method " + methodName );

            environment.addTrackingChange( TrackingField.PERIPHERAL_OPS );
            return method.apply( peripheral, context, this, arguments );
        }

        // IComputerAccess implementation
        @Override
        public synchronized String mount( @Nonnull String desiredLoc, @Nonnull IMount mount, @Nonnull String driveName )
        {
            if( !attached ) throw new NotAttachedException();
            return super.mount( desiredLoc, mount, driveName );
        }

        @Override
        public synchronized String mountWritable( @Nonnull String desiredLoc, @Nonnull IWritableMount mount, @Nonnull String driveName )
        {
            if( !attached ) throw new NotAttachedException();
            return super.mountWritable( desiredLoc, mount, driveName );
        }

        @Override
        public synchronized void unmount( String location )
        {
            if( !attached ) throw new NotAttachedException();
            super.unmount( location );
        }

        @Override
        public int getID()
        {
            if( !attached ) throw new NotAttachedException();
            return super.getID();
        }

        @Override
        public void queueEvent( @Nonnull String event, Object... arguments )
        {
            if( !attached ) throw new NotAttachedException();
            super.queueEvent( event, arguments );
        }

        @Nonnull
        @Override
        public String getAttachmentName()
        {
            if( !attached ) throw new NotAttachedException();
            return side;
        }

        @Nonnull
        @Override
        public Map<String, IPeripheral> getAvailablePeripherals()
        {
            if( !attached ) throw new NotAttachedException();

            Map<String, IPeripheral> peripherals = new HashMap<>();
            for( PeripheralWrapper wrapper : PeripheralAPI.this.peripherals )
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
            if( !attached ) throw new NotAttachedException();

            for( PeripheralWrapper wrapper : peripherals )
            {
                if( wrapper != null && wrapper.isAttached() && wrapper.getAttachmentName().equals( name ) )
                {
                    return wrapper.getPeripheral();
                }
            }
            return null;
        }

        @Nonnull
        @Override
        public IWorkMonitor getMainThreadMonitor()
        {
            if( !attached ) throw new NotAttachedException();
            return super.getMainThreadMonitor();
        }
    }

    private final IAPIEnvironment environment;
    private final PeripheralWrapper[] peripherals = new PeripheralWrapper[6];
    private boolean running;

    public PeripheralAPI( IAPIEnvironment environment )
    {
        this.environment = environment;
        this.environment.setPeripheralChangeListener( this );
        running = false;
    }

    // IPeripheralChangeListener

    @Override
    public void onPeripheralChanged( ComputerSide side, IPeripheral newPeripheral )
    {
        synchronized( peripherals )
        {
            int index = side.ordinal();
            if( peripherals[index] != null )
            {
                // Queue a detachment
                final PeripheralWrapper wrapper = peripherals[index];
                if( wrapper.isAttached() ) wrapper.detach();

                // Queue a detachment event
                environment.queueEvent( "peripheral_detach", side.getName() );
            }

            // Assign the new peripheral
            peripherals[index] = newPeripheral == null ? null
                : new PeripheralWrapper( newPeripheral, side.getName() );

            if( peripherals[index] != null )
            {
                // Queue an attachment
                final PeripheralWrapper wrapper = peripherals[index];
                if( running && !wrapper.isAttached() ) wrapper.attach();

                // Queue an attachment event
                environment.queueEvent( "peripheral", side.getName() );
            }
        }
    }

    @Override
    public String[] getNames()
    {
        return new String[] { "peripheral" };
    }

    @Override
    public void startup()
    {
        synchronized( peripherals )
        {
            running = true;
            for( int i = 0; i < 6; i++ )
            {
                PeripheralWrapper wrapper = peripherals[i];
                if( wrapper != null && !wrapper.isAttached() ) wrapper.attach();
            }
        }
    }

    @Override
    public void shutdown()
    {
        synchronized( peripherals )
        {
            running = false;
            for( int i = 0; i < 6; i++ )
            {
                PeripheralWrapper wrapper = peripherals[i];
                if( wrapper != null && wrapper.isAttached() )
                {
                    wrapper.detach();
                }
            }
        }
    }

    @LuaFunction
    public final boolean isPresent( String sideName )
    {
        ComputerSide side = ComputerSide.valueOfInsensitive( sideName );
        if( side != null )
        {
            synchronized( peripherals )
            {
                PeripheralWrapper p = peripherals[side.ordinal()];
                if( p != null ) return true;
            }
        }
        return false;
    }

    @LuaFunction
    public final Object[] getType( String sideName )
    {
        ComputerSide side = ComputerSide.valueOfInsensitive( sideName );
        if( side == null ) return null;

        synchronized( peripherals )
        {
            PeripheralWrapper p = peripherals[side.ordinal()];
            return p == null ? null : LuaUtil.consArray( p.getType(), p.getAdditionalTypes() );
        }
    }

    @LuaFunction
    public final Object[] hasType( String sideName, String type )
    {
        ComputerSide side = ComputerSide.valueOfInsensitive( sideName );
        if( side == null ) return null;

        synchronized( peripherals )
        {
            PeripheralWrapper p = peripherals[side.ordinal()];
            if( p != null )
            {
                return new Object[] { p.getType().equals( type ) || p.getAdditionalTypes().contains( type ) };
            }
        }
        return null;
    }

    @LuaFunction
    public final Object[] getMethods( String sideName )
    {
        ComputerSide side = ComputerSide.valueOfInsensitive( sideName );
        if( side == null ) return null;

        synchronized( peripherals )
        {
            PeripheralWrapper p = peripherals[side.ordinal()];
            if( p != null ) return new Object[] { p.getMethods() };
        }
        return null;
    }

    @LuaFunction
    public final MethodResult call( ILuaContext context, IArguments args ) throws LuaException
    {
        ComputerSide side = ComputerSide.valueOfInsensitive( args.getString( 0 ) );
        String methodName = args.getString( 1 );
        IArguments methodArgs = args.drop( 2 );

        if( side == null ) throw new LuaException( "No peripheral attached" );

        PeripheralWrapper p;
        synchronized( peripherals )
        {
            p = peripherals[side.ordinal()];
        }
        if( p == null ) throw new LuaException( "No peripheral attached" );

        try
        {
            return p.call( context, methodName, methodArgs ).adjustError( 1 );
        }
        catch( LuaException e )
        {
            // We increase the error level by one in order to shift the error level to where peripheral.call was
            // invoked. It would be possible to do it in Lua code, but would add significantly more overhead.
            if( e.getLevel() > 0 ) throw new FastLuaException( e.getMessage(), e.getLevel() + 1 );
            throw e;
        }
    }

    public static Map<String, PeripheralMethod> getMethods( IPeripheral peripheral )
    {
        String[] dynamicMethods = peripheral instanceof IDynamicPeripheral
            ? Objects.requireNonNull( ((IDynamicPeripheral) peripheral).getMethodNames(), "Peripheral methods cannot be null" )
            : LuaMethod.EMPTY_METHODS;

        List<NamedMethod<PeripheralMethod>> methods = PeripheralMethod.GENERATOR.getMethods( peripheral.getClass() );

        Map<String, PeripheralMethod> methodMap = new HashMap<>( methods.size() + dynamicMethods.length );
        for( int i = 0; i < dynamicMethods.length; i++ )
        {
            methodMap.put( dynamicMethods[i], PeripheralMethod.DYNAMIC.get( i ) );
        }
        for( NamedMethod<PeripheralMethod> method : methods )
        {
            methodMap.put( method.getName(), method.getMethod() );
        }
        return methodMap;
    }
}
