/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.core.computer;

import java.util.Arrays;
import java.util.Iterator;

import javax.annotation.Nonnull;

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
public final class Environment implements IAPIEnvironment {
    private final Computer computer;
    private final int[] internalOutput = new int[ComputerSide.COUNT];
    private final int[] internalBundledOutput = new int[ComputerSide.COUNT];
    private final int[] externalOutput = new int[ComputerSide.COUNT];
    private final int[] externalBundledOutput = new int[ComputerSide.COUNT];
    private final int[] input = new int[ComputerSide.COUNT];
    private final int[] bundledInput = new int[ComputerSide.COUNT];
    private final IPeripheral[] peripherals = new IPeripheral[ComputerSide.COUNT];
    private final Int2ObjectMap<Timer> timers = new Int2ObjectOpenHashMap<>();
    private boolean internalOutputChanged = false;
    private boolean inputChanged = false;
    private IPeripheralChangeListener peripheralListener = null;
    private int nextTimerToken = 0;

    Environment(Computer computer) {
        this.computer = computer;
    }

    @Override
    public int getComputerID() {
        return this.computer.assignID();
    }

    @Nonnull
    @Override
    public IComputerEnvironment getComputerEnvironment() {
        return this.computer.getComputerEnvironment();
    }

    @Nonnull
    @Override
    public IWorkMonitor getMainThreadMonitor() {
        return this.computer.getMainThreadMonitor();
    }

    @Nonnull
    @Override
    public Terminal getTerminal() {
        return this.computer.getTerminal();
    }

    @Override
    public FileSystem getFileSystem() {
        return this.computer.getFileSystem();
    }

    @Override
    public void shutdown() {
        this.computer.shutdown();
    }

    @Override
    public void reboot() {
        this.computer.reboot();
    }

    @Override
    public void queueEvent(String event, Object... args) {
        this.computer.queueEvent(event, args);
    }

    @Override
    public void setOutput(ComputerSide side, int output) {
        int index = side.ordinal();
        synchronized (this.internalOutput) {
            if (this.internalOutput[index] != output) {
                this.internalOutput[index] = output;
                this.internalOutputChanged = true;
            }
        }
    }

    @Override
    public int getOutput(ComputerSide side) {
        synchronized (this.internalOutput) {
            return this.computer.isOn() ? this.internalOutput[side.ordinal()] : 0;
        }
    }

    @Override
    public int getInput(ComputerSide side) {
        return this.input[side.ordinal()];
    }

    @Override
    public void setBundledOutput(ComputerSide side, int output) {
        int index = side.ordinal();
        synchronized (this.internalOutput) {
            if (this.internalBundledOutput[index] != output) {
                this.internalBundledOutput[index] = output;
                this.internalOutputChanged = true;
            }
        }
    }

    @Override
    public int getBundledOutput(ComputerSide side) {
        synchronized (this.internalOutput) {
            return this.computer.isOn() ? this.internalBundledOutput[side.ordinal()] : 0;
        }
    }

    @Override
    public int getBundledInput(ComputerSide side) {
        return this.bundledInput[side.ordinal()];
    }

    @Override
    public void setPeripheralChangeListener(IPeripheralChangeListener listener) {
        synchronized (this.peripherals) {
            this.peripheralListener = listener;
        }
    }

    @Override
    public IPeripheral getPeripheral(ComputerSide side) {
        synchronized (this.peripherals) {
            return this.peripherals[side.ordinal()];
        }
    }

    @Override
    public String getLabel() {
        return this.computer.getLabel();
    }

    @Override
    public void setLabel(String label) {
        this.computer.setLabel(label);
    }

    @Override
    public int startTimer(long ticks) {
        synchronized (this.timers) {
            this.timers.put(this.nextTimerToken, new Timer(ticks));
            return this.nextTimerToken++;
        }
    }

    @Override
    public void cancelTimer(int id) {
        synchronized (this.timers) {
            this.timers.remove(id);
        }
    }

    @Override
    public void addTrackingChange(@Nonnull TrackingField field, long change) {
        Tracking.addValue(this.computer, field, change);
    }

    public int getExternalRedstoneOutput(ComputerSide side) {
        return this.computer.isOn() ? this.externalOutput[side.ordinal()] : 0;
    }

    public int getExternalBundledRedstoneOutput(ComputerSide side) {
        return this.computer.isOn() ? this.externalBundledOutput[side.ordinal()] : 0;
    }

    public void setRedstoneInput(ComputerSide side, int level) {
        int index = side.ordinal();
        if (this.input[index] != level) {
            this.input[index] = level;
            this.inputChanged = true;
        }
    }

    public void setBundledRedstoneInput(ComputerSide side, int combination) {
        int index = side.ordinal();
        if (this.bundledInput[index] != combination) {
            this.bundledInput[index] = combination;
            this.inputChanged = true;
        }
    }

    /**
     * Called when the computer starts up or shuts down, to reset any internal state.
     *
     * @see ILuaAPI#startup()
     * @see ILuaAPI#shutdown()
     */
    void reset() {
        synchronized (this.timers) {
            this.timers.clear();
        }
    }

    /**
     * Called on the main thread to update the internal state of the computer.
     */
    void tick() {
        if (this.inputChanged) {
            this.inputChanged = false;
            this.queueEvent("redstone");
        }

        synchronized (this.timers) {
            // Countdown all of our active timers
            Iterator<Int2ObjectMap.Entry<Timer>> it = this.timers.int2ObjectEntrySet()
                                                                 .iterator();
            while (it.hasNext()) {
                Int2ObjectMap.Entry<Timer> entry = it.next();
                Timer timer = entry.getValue();
                timer.ticksLeft--;
                if (timer.ticksLeft <= 0) {
                    // Queue the "timer" event
                    this.queueEvent(TIMER_EVENT, entry.getIntKey());
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
    boolean updateOutput() {
        // Mark output as changed if the internal redstone has changed
        synchronized (this.internalOutput) {
            if (!this.internalOutputChanged) {
                return false;
            }

            boolean changed = false;

            for (int i = 0; i < ComputerSide.COUNT; i++) {
                if (this.externalOutput[i] != this.internalOutput[i]) {
                    this.externalOutput[i] = this.internalOutput[i];
                    changed = true;
                }

                if (this.externalBundledOutput[i] != this.internalBundledOutput[i]) {
                    this.externalBundledOutput[i] = this.internalBundledOutput[i];
                    changed = true;
                }
            }

            this.internalOutputChanged = false;

            return changed;
        }
    }

    void resetOutput() {
        // Reset redstone output
        synchronized (this.internalOutput) {
            Arrays.fill(this.internalOutput, 0);
            Arrays.fill(this.internalBundledOutput, 0);
            this.internalOutputChanged = true;
        }
    }

    public void setPeripheral(ComputerSide side, IPeripheral peripheral) {
        synchronized (this.peripherals) {
            int index = side.ordinal();
            IPeripheral existing = this.peripherals[index];
            if ((existing == null && peripheral != null) || (existing != null && peripheral == null) || (existing != null && !existing.equals(peripheral))) {
                this.peripherals[index] = peripheral;
                if (this.peripheralListener != null) {
                    this.peripheralListener.onPeripheralChanged(side, peripheral);
                }
            }
        }
    }

    private static class Timer {
        long ticksLeft;

        Timer(long ticksLeft) {
            this.ticksLeft = ticksLeft;
        }
    }
}
