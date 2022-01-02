/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.peripheral.monitor;

import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.lua.LuaValues;
import dan200.computercraft.api.peripheral.IComputerAccess;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.core.apis.TermMethods;
import dan200.computercraft.core.terminal.Terminal;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Monitors are a block which act as a terminal, displaying information on one side. This allows them to be read and
 * interacted with in-world without opening a GUI.
 *
 * Monitors act as @{term.Redirect|terminal redirects} and so expose the same methods, as well as several additional
 * ones, which are documented below.
 *
 * Like computers, monitors come in both normal (no colour) and advanced (colour) varieties.
 *
 * @cc.module monitor
 * @cc.usage Write "Hello, world!" to an adjacent monitor:
 *
 * <pre>{@code
 * local monitor = peripheral.find("monitor")
 * monitor.setCursorPos(1, 1)
 * monitor.write("Hello, world!")
 * }</pre>
 */
public class MonitorPeripheral extends TermMethods implements IPeripheral
{
    private final TileMonitor monitor;

    public MonitorPeripheral( TileMonitor monitor )
    {
        this.monitor = monitor;
    }

    @Nonnull
    @Override
    public String getType()
    {
        return "monitor";
    }

    /**
     * Set the scale of this monitor. A larger scale will result in the monitor having a lower resolution, but display
     * text much larger.
     *
     * @param scaleArg The monitor's scale. This must be a multiple of 0.5 between 0.5 and 5.
     * @throws LuaException If the scale is out of range.
     * @see #getTextScale()
     */
    @LuaFunction
    public final void setTextScale( double scaleArg ) throws LuaException
    {
        int scale = (int) (LuaValues.checkFinite( 0, scaleArg ) * 2.0);
        if( scale < 1 || scale > 10 ) throw new LuaException( "Expected number in range 0.5-5" );
        getMonitor().setTextScale( scale );
    }

    /**
     * Get the monitor's current text scale.
     *
     * @return The monitor's current scale.
     * @throws LuaException If the monitor cannot be found.
     * @cc.since 1.81.0
     */
    @LuaFunction
    public final double getTextScale() throws LuaException
    {
        return getMonitor().getTextScale() / 2.0;
    }

    @Override
    public void attach( @Nonnull IComputerAccess computer )
    {
        monitor.addComputer( computer );
    }

    @Override
    public void detach( @Nonnull IComputerAccess computer )
    {
        monitor.removeComputer( computer );
    }

    @Override
    public boolean equals( IPeripheral other )
    {
        return other instanceof MonitorPeripheral && monitor == ((MonitorPeripheral) other).monitor;
    }

    @Nonnull
    private ServerMonitor getMonitor() throws LuaException
    {
        ServerMonitor monitor = this.monitor.getCachedServerMonitor();
        if( monitor == null ) throw new LuaException( "Monitor has been detached" );
        return monitor;
    }

    @Nonnull
    @Override
    public Terminal getTerminal() throws LuaException
    {
        Terminal terminal = getMonitor().getTerminal();
        if( terminal == null ) throw new LuaException( "Monitor has been detached" );
        return terminal;
    }

    @Override
    public boolean isColour() throws LuaException
    {
        return getMonitor().isColour();
    }

    @Nullable
    @Override
    public Object getTarget()
    {
        return monitor;
    }
}
