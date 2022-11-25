/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.core.apis;

import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.api.peripheral.WorkMonitor;
import dan200.computercraft.core.computer.ComputerEnvironment;
import dan200.computercraft.core.computer.ComputerSide;
import dan200.computercraft.core.computer.GlobalEnvironment;
import dan200.computercraft.core.filesystem.FileSystem;
import dan200.computercraft.core.metrics.Metric;
import dan200.computercraft.core.terminal.Terminal;

import javax.annotation.Nullable;

public interface IAPIEnvironment {
    String TIMER_EVENT = "timer";

    @FunctionalInterface
    interface IPeripheralChangeListener {
        void onPeripheralChanged(ComputerSide side, @Nullable IPeripheral newPeripheral);
    }

    int getComputerID();

    ComputerEnvironment getComputerEnvironment();

    GlobalEnvironment getGlobalEnvironment();

    WorkMonitor getMainThreadMonitor();

    Terminal getTerminal();

    FileSystem getFileSystem();

    void shutdown();

    void reboot();

    void queueEvent(String event, @Nullable Object... args);

    void setOutput(ComputerSide side, int output);

    int getOutput(ComputerSide side);

    int getInput(ComputerSide side);

    void setBundledOutput(ComputerSide side, int output);

    int getBundledOutput(ComputerSide side);

    int getBundledInput(ComputerSide side);

    void setPeripheralChangeListener(@Nullable IPeripheralChangeListener listener);

    @Nullable
    IPeripheral getPeripheral(ComputerSide side);

    @Nullable
    String getLabel();

    void setLabel(@Nullable String label);

    int startTimer(long ticks);

    void cancelTimer(int id);

    void observe(Metric.Event event, long change);

    void observe(Metric.Counter counter);
}
