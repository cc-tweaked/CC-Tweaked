// Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
//
// SPDX-License-Identifier: LicenseRef-CCPL

package dan200.computercraft.core.computer;

import dan200.computercraft.api.lua.ILuaAPI;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.api.peripheral.WorkMonitor;
import dan200.computercraft.core.apis.IAPIEnvironment;
import dan200.computercraft.core.filesystem.FileSystem;
import dan200.computercraft.core.metrics.MetricsObserver;
import dan200.computercraft.core.terminal.Terminal;
import dan200.computercraft.core.util.PeripheralHelpers;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

import javax.annotation.Nullable;
import java.util.Iterator;

/**
 * Represents the "environment" that a {@link Computer} exists in.
 * <p>
 * This handles storing and updating of peripherals and timers.
 *
 * <h1>Peripheral</h1>
 * We also keep track of peripherals. These are read on both threads, and only written on the main thread.
 */
public final class Environment implements IAPIEnvironment {
    private final Computer computer;
    private final ComputerEnvironment environment;
    private final MetricsObserver metrics;

    private final IPeripheral[] peripherals = new IPeripheral[ComputerSide.COUNT];
    private @Nullable IPeripheralChangeListener peripheralListener = null;

    private final Int2ObjectMap<Timer> timers = new Int2ObjectOpenHashMap<>();
    private int nextTimerToken = 0;

    Environment(Computer computer, ComputerEnvironment environment) {
        this.computer = computer;
        this.environment = environment;
        metrics = environment.getMetrics();
    }

    @Override
    public int getComputerID() {
        return computer.getID();
    }

    @Override
    public ComputerEnvironment getComputerEnvironment() {
        return environment;
    }

    @Override
    public GlobalEnvironment getGlobalEnvironment() {
        return computer.getGlobalEnvironment();
    }

    @Override
    public WorkMonitor getMainThreadMonitor() {
        return computer.getMainThreadMonitor();
    }

    @Override
    public Terminal getTerminal() {
        return computer.getTerminal();
    }

    @Override
    public FileSystem getFileSystem() {
        return computer.getFileSystem();
    }

    @Override
    public void shutdown() {
        computer.shutdown();
    }

    @Override
    public void reboot() {
        computer.reboot();
    }

    @Override
    public void queueEvent(String event, @Nullable Object... args) {
        computer.queueEvent(event, args);
    }

    /**
     * Called when the computer starts up or shuts down, to reset any internal state.
     *
     * @see ILuaAPI#startup()
     * @see ILuaAPI#shutdown()
     */
    void reset() {
        synchronized (timers) {
            timers.clear();
        }
    }

    /**
     * Called on the main thread to update the internal state of the computer.
     */
    void tick() {
        synchronized (timers) {
            // Countdown all of our active timers
            Iterator<Int2ObjectMap.Entry<Timer>> it = timers.int2ObjectEntrySet().iterator();
            while (it.hasNext()) {
                var entry = it.next();
                var timer = entry.getValue();
                timer.ticksLeft--;
                if (timer.ticksLeft <= 0) {
                    // Queue the "timer" event
                    queueEvent(TIMER_EVENT, entry.getIntKey());
                    it.remove();
                }
            }
        }
    }

    @Nullable
    @Override
    public IPeripheral getPeripheral(ComputerSide side) {
        synchronized (peripherals) {
            return peripherals[side.ordinal()];
        }
    }

    public void setPeripheral(ComputerSide side, @Nullable IPeripheral peripheral) {
        synchronized (peripherals) {
            var index = side.ordinal();
            var existing = peripherals[index];
            if (!PeripheralHelpers.equals(existing, peripheral)) {
                peripherals[index] = peripheral;
                if (peripheralListener != null) peripheralListener.onPeripheralChanged(side, peripheral);
            }
        }
    }

    @Override
    public void setPeripheralChangeListener(@Nullable IPeripheralChangeListener listener) {
        synchronized (peripherals) {
            peripheralListener = listener;
        }
    }

    @Nullable
    @Override
    public String getLabel() {
        return computer.getLabel();
    }

    @Override
    public void setLabel(@Nullable String label) {
        computer.setLabel(label);
    }

    @Override
    public int startTimer(long ticks) {
        synchronized (timers) {
            timers.put(nextTimerToken, new Timer(ticks));
            return nextTimerToken++;
        }
    }

    @Override
    public void cancelTimer(int id) {
        synchronized (timers) {
            timers.remove(id);
        }
    }

    @Override
    public MetricsObserver metrics() {
        return metrics;
    }

    private static class Timer {
        long ticksLeft;

        Timer(long ticksLeft) {
            this.ticksLeft = ticksLeft;
        }
    }
}
