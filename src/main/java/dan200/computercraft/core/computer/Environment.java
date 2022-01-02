/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.core.computer;

import dan200.computercraft.api.lua.ILuaAPI;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.api.peripheral.IWorkMonitor;
import dan200.computercraft.core.apis.IAPIEnvironment;
import dan200.computercraft.core.filesystem.FileSystem;
import dan200.computercraft.core.terminal.Terminal;
import dan200.computercraft.core.tracking.Tracking;
import dan200.computercraft.core.tracking.TrackingField;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.Iterator;

/**
 * Represents the "environment" that a {@link Computer} exists in.
 *
 * This handles storing and updating of peripherals and redstone.
 *
 * <h1>Redstone</h1>
 * We holds three kinds of arrays for redstone, in normal and bundled versions:
 * <ul>
 * <li>{@link #internalOutput} is the redstone output which the computer has currently set. This is read on both
 * threads, and written on the computer thread.</li>
 * <li>{@link #externalOutput} is the redstone output currently propagated to the world. This is only read and written
 * on the main thread.</li>
 * <li>{@link #input} is the redstone input from external sources. This is read on both threads, and written on the main
 * thread.</li>
 * </ul>
 *
 * <h1>Peripheral</h1>
 * We also keep track of peripherals. These are read on both threads, and only written on the main thread.
 */
public final class Environment implements IAPIEnvironment
{
    private final Computer computer;

    private boolean internalOutputChanged = false;
    private final int[] internalOutput = new int[ComputerSide.COUNT];
    private final int[] internalBundledOutput = new int[ComputerSide.COUNT];

    private final int[] externalOutput = new int[ComputerSide.COUNT];
    private final int[] externalBundledOutput = new int[ComputerSide.COUNT];

    private boolean inputChanged = false;
    private final int[] input = new int[ComputerSide.COUNT];
    private final int[] bundledInput = new int[ComputerSide.COUNT];

    private final IPeripheral[] peripherals = new IPeripheral[ComputerSide.COUNT];
    private IPeripheralChangeListener peripheralListener = null;

    private final Int2ObjectMap<Timer> timers = new Int2ObjectOpenHashMap<>();
    private int nextTimerToken = 0;

    Environment( Computer computer )
    {
        this.computer = computer;
    }

    @Override
    public int getComputerID()
    {
        return computer.assignID();
    }

    @Nonnull
    @Override
    public IComputerEnvironment getComputerEnvironment()
    {
        return computer.getComputerEnvironment();
    }

    @Nonnull
    @Override
    public IWorkMonitor getMainThreadMonitor()
    {
        return computer.getMainThreadMonitor();
    }

    @Nonnull
    @Override
    public Terminal getTerminal()
    {
        return computer.getTerminal();
    }

    @Override
    public FileSystem getFileSystem()
    {
        return computer.getFileSystem();
    }

    @Override
    public void shutdown()
    {
        computer.shutdown();
    }

    @Override
    public void reboot()
    {
        computer.reboot();
    }

    @Override
    public void queueEvent( String event, Object... args )
    {
        computer.queueEvent( event, args );
    }

    @Override
    public int getInput( ComputerSide side )
    {
        return input[side.ordinal()];
    }

    @Override
    public int getBundledInput( ComputerSide side )
    {
        return bundledInput[side.ordinal()];
    }

    @Override
    public void setOutput( ComputerSide side, int output )
    {
        int index = side.ordinal();
        synchronized( internalOutput )
        {
            if( internalOutput[index] != output )
            {
                internalOutput[index] = output;
                internalOutputChanged = true;
            }
        }
    }

    @Override
    public int getOutput( ComputerSide side )
    {
        synchronized( internalOutput )
        {
            return computer.isOn() ? internalOutput[side.ordinal()] : 0;
        }
    }

    @Override
    public void setBundledOutput( ComputerSide side, int output )
    {
        int index = side.ordinal();
        synchronized( internalOutput )
        {
            if( internalBundledOutput[index] != output )
            {
                internalBundledOutput[index] = output;
                internalOutputChanged = true;
            }
        }
    }

    @Override
    public int getBundledOutput( ComputerSide side )
    {
        synchronized( internalOutput )
        {
            return computer.isOn() ? internalBundledOutput[side.ordinal()] : 0;
        }
    }

    public int getExternalRedstoneOutput( ComputerSide side )
    {
        return computer.isOn() ? externalOutput[side.ordinal()] : 0;
    }

    public int getExternalBundledRedstoneOutput( ComputerSide side )
    {
        return computer.isOn() ? externalBundledOutput[side.ordinal()] : 0;
    }

    public void setRedstoneInput( ComputerSide side, int level )
    {
        int index = side.ordinal();
        if( input[index] != level )
        {
            input[index] = level;
            inputChanged = true;
        }
    }

    public void setBundledRedstoneInput( ComputerSide side, int combination )
    {
        int index = side.ordinal();
        if( bundledInput[index] != combination )
        {
            bundledInput[index] = combination;
            inputChanged = true;
        }
    }

    /**
     * Called when the computer starts up or shuts down, to reset any internal state.
     *
     * @see ILuaAPI#startup()
     * @see ILuaAPI#shutdown()
     */
    void reset()
    {
        synchronized( timers )
        {
            timers.clear();
        }
    }

    /**
     * Called on the main thread to update the internal state of the computer.
     */
    void tick()
    {
        if( inputChanged )
        {
            inputChanged = false;
            queueEvent( "redstone" );
        }

        synchronized( timers )
        {
            // Countdown all of our active timers
            Iterator<Int2ObjectMap.Entry<Timer>> it = timers.int2ObjectEntrySet().iterator();
            while( it.hasNext() )
            {
                Int2ObjectMap.Entry<Timer> entry = it.next();
                Timer timer = entry.getValue();
                timer.ticksLeft--;
                if( timer.ticksLeft <= 0 )
                {
                    // Queue the "timer" event
                    queueEvent( TIMER_EVENT, entry.getIntKey() );
                    it.remove();
                }
            }
        }
    }

    /**
     * Called on the main thread to propagate the internal outputs to the external ones.
     *
     * @return If the outputs have changed.
     */
    boolean updateOutput()
    {
        // Mark output as changed if the internal redstone has changed
        synchronized( internalOutput )
        {
            if( !internalOutputChanged ) return false;

            boolean changed = false;

            for( int i = 0; i < ComputerSide.COUNT; i++ )
            {
                if( externalOutput[i] != internalOutput[i] )
                {
                    externalOutput[i] = internalOutput[i];
                    changed = true;
                }

                if( externalBundledOutput[i] != internalBundledOutput[i] )
                {
                    externalBundledOutput[i] = internalBundledOutput[i];
                    changed = true;
                }
            }

            internalOutputChanged = false;

            return changed;
        }
    }

    void resetOutput()
    {
        // Reset redstone output
        synchronized( internalOutput )
        {
            Arrays.fill( internalOutput, 0 );
            Arrays.fill( internalBundledOutput, 0 );
            internalOutputChanged = true;
        }
    }

    @Override
    public IPeripheral getPeripheral( ComputerSide side )
    {
        synchronized( peripherals )
        {
            return peripherals[side.ordinal()];
        }
    }

    public void setPeripheral( ComputerSide side, IPeripheral peripheral )
    {
        synchronized( peripherals )
        {
            int index = side.ordinal();
            IPeripheral existing = peripherals[index];
            if( (existing == null && peripheral != null) ||
                (existing != null && peripheral == null) ||
                (existing != null && !existing.equals( peripheral )) )
            {
                peripherals[index] = peripheral;
                if( peripheralListener != null ) peripheralListener.onPeripheralChanged( side, peripheral );
            }
        }
    }

    @Override
    public void setPeripheralChangeListener( IPeripheralChangeListener listener )
    {
        synchronized( peripherals )
        {
            peripheralListener = listener;
        }
    }

    @Override
    public String getLabel()
    {
        return computer.getLabel();
    }

    @Override
    public void setLabel( String label )
    {
        computer.setLabel( label );
    }

    @Override
    public int startTimer( long ticks )
    {
        synchronized( timers )
        {
            timers.put( nextTimerToken, new Timer( ticks ) );
            return nextTimerToken++;
        }
    }

    @Override
    public void cancelTimer( int id )
    {
        synchronized( timers )
        {
            timers.remove( id );
        }
    }

    @Override
    public void addTrackingChange( @Nonnull TrackingField field, long change )
    {
        Tracking.addValue( computer, field, change );
    }

    private static class Timer
    {
        long ticksLeft;

        Timer( long ticksLeft )
        {
            this.ticksLeft = ticksLeft;
        }
    }
}
