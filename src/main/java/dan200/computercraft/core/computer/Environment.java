/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.core.computer;

import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.api.peripheral.IWorkMonitor;
import dan200.computercraft.core.apis.IAPIEnvironment;
import dan200.computercraft.core.filesystem.FileSystem;
import dan200.computercraft.core.terminal.Terminal;
import dan200.computercraft.core.tracking.Tracking;
import dan200.computercraft.core.tracking.TrackingField;

import javax.annotation.Nonnull;
import java.util.Arrays;

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
    private final int[] internalOutput = new int[SIDE_COUNT];
    private final int[] internalBundledOutput = new int[SIDE_COUNT];

    private final int[] externalOutput = new int[SIDE_COUNT];
    private final int[] externalBundledOutput = new int[SIDE_COUNT];

    private boolean inputChanged = false;
    private final int[] input = new int[SIDE_COUNT];
    private final int[] bundledInput = new int[SIDE_COUNT];

    private final IPeripheral[] peripherals = new IPeripheral[SIDE_COUNT];
    private IPeripheralChangeListener peripheralListener = null;

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
    public void queueEvent( String event, Object[] args )
    {
        computer.queueEvent( event, args );
    }

    @Override
    public int getInput( int side )
    {
        return input[side];
    }

    @Override
    public int getBundledInput( int side )
    {
        return bundledInput[side];
    }

    @Override
    public void setOutput( int side, int output )
    {
        synchronized( internalOutput )
        {
            if( internalOutput[side] != output )
            {
                internalOutput[side] = output;
                internalOutputChanged = true;
            }
        }
    }

    @Override
    public int getOutput( int side )
    {
        synchronized( internalOutput )
        {
            return computer.isOn() ? internalOutput[side] : 0;
        }
    }

    @Override
    public void setBundledOutput( int side, int output )
    {
        synchronized( internalOutput )
        {
            if( internalBundledOutput[side] != output )
            {
                internalBundledOutput[side] = output;
                internalOutputChanged = true;
            }
        }
    }

    @Override
    public int getBundledOutput( int side )
    {
        synchronized( internalOutput )
        {
            return computer.isOn() ? internalBundledOutput[side] : 0;
        }
    }

    public int getExternalRedstoneOutput( int side )
    {
        return computer.isOn() ? externalOutput[side] : 0;
    }

    public int getExternalBundledRedstoneOutput( int side )
    {
        return computer.isOn() ? externalBundledOutput[side] : 0;
    }

    public void setRedstoneInput( int side, int level )
    {
        if( input[side] != level )
        {
            input[side] = level;
            inputChanged = true;
        }
    }

    public void setBundledRedstoneInput( int side, int combination )
    {
        if( bundledInput[side] != combination )
        {
            bundledInput[side] = combination;
            inputChanged = true;
        }
    }

    /**
     * Called on the main thread to update the internal state of the computer.
     *
     * This just queues a {@code redstone} event if the input has changed.
     */
    void update()
    {
        if( inputChanged )
        {
            inputChanged = false;
            queueEvent( "redstone", null );
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

            for( int i = 0; i < SIDE_COUNT; i++ )
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
    public IPeripheral getPeripheral( int side )
    {
        synchronized( peripherals )
        {
            return peripherals[side];
        }
    }

    public void setPeripheral( int side, IPeripheral peripheral )
    {
        synchronized( peripherals )
        {
            IPeripheral existing = peripherals[side];
            if( (existing == null && peripheral != null) ||
                (existing != null && peripheral == null) ||
                (existing != null && !existing.equals( peripheral )) )
            {
                peripherals[side] = peripheral;
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
    public void addTrackingChange( @Nonnull TrackingField field, long change )
    {
        Tracking.addValue( computer, field, change );
    }
}
