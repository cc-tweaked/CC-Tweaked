// Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
//
// SPDX-License-Identifier: LicenseRef-CCPL

package dan200.computercraft.shared.peripheral.monitor;

import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.lua.LuaValues;
import dan200.computercraft.api.peripheral.IComputerAccess;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.core.apis.TermMethods;
import dan200.computercraft.core.terminal.Terminal;

import javax.annotation.Nullable;

/**
 * Monitors are a block which act as a terminal, displaying information on one side. This allows them to be read and
 * interacted with in-world without opening a GUI.
 * <p>
 * Monitors act as [terminal redirects][`term.Redirect`] and so expose the same methods, as well as several additional
 * ones, which are documented below.
 * <p>
 * Like computers, monitors come in both normal (no colour) and advanced (colour) varieties.
 * <p>
 * ## Recipes
 * <div class="recipe-container">
 *     <mc-recipe recipe="computercraft:monitor_normal"></mc-recipe>
 *     <mc-recipe recipe="computercraft:monitor_advanced"></mc-recipe>
 * </div>
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
public class MonitorPeripheral extends TermMethods implements IPeripheral {
    private final MonitorBlockEntity monitor;

    public MonitorPeripheral(MonitorBlockEntity monitor) {
        this.monitor = monitor;
    }

    @Override
    public String getType() {
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
    public final void setTextScale(double scaleArg) throws LuaException {
        var scale = (int) (LuaValues.checkFinite(0, scaleArg) * 2.0);
        if (scale < 1 || scale > 10) throw new LuaException("Expected number in range 0.5-5");
        getMonitor().setTextScale(scale);
    }

    /**
     * Get the monitor's current text scale.
     *
     * @return The monitor's current scale.
     * @throws LuaException If the monitor cannot be found.
     * @cc.since 1.81.0
     */
    @LuaFunction
    public final double getTextScale() throws LuaException {
        return getMonitor().getTextScale() / 2.0;
    }

    @Override
    public void attach(IComputerAccess computer) {
        monitor.addComputer(computer);
    }

    @Override
    public void detach(IComputerAccess computer) {
        monitor.removeComputer(computer);
    }

    @Override
    public boolean equals(@Nullable IPeripheral other) {
        return other instanceof MonitorPeripheral && monitor == ((MonitorPeripheral) other).monitor;
    }

    private ServerMonitor getMonitor() throws LuaException {
        var monitor = this.monitor.getCachedServerMonitor();
        if (monitor == null) throw new LuaException("Monitor has been detached");
        return monitor;
    }

    @Override
    public Terminal getTerminal() throws LuaException {
        Terminal terminal = getMonitor().getTerminal();
        if (terminal == null) throw new LuaException("Monitor has been detached");
        return terminal;
    }

    @Nullable
    @Override
    public Object getTarget() {
        return monitor;
    }
}
