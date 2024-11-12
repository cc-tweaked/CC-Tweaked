// Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
//
// SPDX-License-Identifier: LicenseRef-CCPL

package dan200.computercraft.core.apis;

import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.api.peripheral.WorkMonitor;
import dan200.computercraft.core.computer.ComputerEnvironment;
import dan200.computercraft.core.computer.ComputerSide;
import dan200.computercraft.core.computer.GlobalEnvironment;
import dan200.computercraft.core.filesystem.FileSystem;
import dan200.computercraft.core.metrics.Metric;
import dan200.computercraft.core.metrics.MetricsObserver;
import dan200.computercraft.core.metrics.OperationTimer;
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

    void setPeripheralChangeListener(@Nullable IPeripheralChangeListener listener);

    @Nullable
    IPeripheral getPeripheral(ComputerSide side);

    @Nullable
    String getLabel();

    void setLabel(@Nullable String label);

    int startTimer(long ticks);

    void cancelTimer(int id);

    MetricsObserver metrics();

    default void observe(Metric.Event event, long change) {
        metrics().observe(event, change);
    }

    default void observe(Metric.Counter counter) {
        metrics().observe(counter);
    }

    default OperationTimer time(Metric.Event event) {
        return OperationTimer.start(metrics(), event);
    }
}
