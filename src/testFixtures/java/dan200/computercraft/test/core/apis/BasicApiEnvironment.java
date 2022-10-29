/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.test.core.apis;

import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.api.peripheral.IWorkMonitor;
import dan200.computercraft.core.apis.IAPIEnvironment;
import dan200.computercraft.core.computer.ComputerEnvironment;
import dan200.computercraft.core.computer.ComputerSide;
import dan200.computercraft.core.computer.GlobalEnvironment;
import dan200.computercraft.core.filesystem.FileSystem;
import dan200.computercraft.core.metrics.Metric;
import dan200.computercraft.core.terminal.Terminal;
import dan200.computercraft.test.core.computer.BasicEnvironment;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public abstract class BasicApiEnvironment implements IAPIEnvironment
{
    private final BasicEnvironment environment;
    private @Nullable String label;

    public BasicApiEnvironment( BasicEnvironment environment )
    {
        this.environment = environment;
    }

    @Override
    public int getComputerID()
    {
        return 0;
    }

    @Nonnull
    @Override
    public ComputerEnvironment getComputerEnvironment()
    {
        return environment;
    }

    @Nonnull
    @Override
    public GlobalEnvironment getGlobalEnvironment()
    {
        return environment;
    }

    @Nonnull
    @Override
    public IWorkMonitor getMainThreadMonitor()
    {
        throw new IllegalStateException( "Main thread monitor not available" );
    }

    @Nonnull
    @Override
    public Terminal getTerminal()
    {
        throw new IllegalStateException( "Terminal not available" );
    }

    @Override
    public FileSystem getFileSystem()
    {
        throw new IllegalStateException( "Filesystem not available" );
    }

    @Override
    public void shutdown()
    {
    }

    @Override
    public void reboot()
    {
    }

    @Override
    public void setOutput( ComputerSide side, int output )
    {
    }

    @Override
    public int getOutput( ComputerSide side )
    {
        return 0;
    }

    @Override
    public int getInput( ComputerSide side )
    {
        return 0;
    }

    @Override
    public void setBundledOutput( ComputerSide side, int output )
    {
    }

    @Override
    public int getBundledOutput( ComputerSide side )
    {
        return 0;
    }

    @Override
    public int getBundledInput( ComputerSide side )
    {
        return 0;
    }

    @Override
    public void setPeripheralChangeListener( @Nullable IPeripheralChangeListener listener )
    {
    }

    @Nullable
    @Override
    public IPeripheral getPeripheral( ComputerSide side )
    {
        return null;
    }

    @Nullable
    @Override
    public String getLabel()
    {
        return label;
    }

    @Override
    public void setLabel( @Nullable String label )
    {
        this.label = label;
    }

    @Override
    public int startTimer( long ticks )
    {
        throw new IllegalStateException( "Cannot start timers" );
    }

    @Override
    public void cancelTimer( int id )
    {
    }

    @Override
    public void observe( @Nonnull Metric.Event summary, long value )
    {
    }

    @Override
    public void observe( @Nonnull Metric.Counter counter )
    {
    }
}
