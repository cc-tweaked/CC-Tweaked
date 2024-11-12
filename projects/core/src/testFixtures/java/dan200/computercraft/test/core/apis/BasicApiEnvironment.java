// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.test.core.apis;

import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.api.peripheral.WorkMonitor;
import dan200.computercraft.core.apis.IAPIEnvironment;
import dan200.computercraft.core.computer.ComputerEnvironment;
import dan200.computercraft.core.computer.ComputerSide;
import dan200.computercraft.core.computer.GlobalEnvironment;
import dan200.computercraft.core.filesystem.FileSystem;
import dan200.computercraft.core.metrics.MetricsObserver;
import dan200.computercraft.core.terminal.Terminal;
import dan200.computercraft.test.core.computer.BasicEnvironment;

import javax.annotation.Nullable;

public abstract class BasicApiEnvironment implements IAPIEnvironment {
    private final BasicEnvironment environment;
    private @Nullable String label;

    public BasicApiEnvironment(BasicEnvironment environment) {
        this.environment = environment;
    }

    @Override
    public int getComputerID() {
        return 0;
    }

    @Override
    public ComputerEnvironment getComputerEnvironment() {
        return environment;
    }

    @Override
    public GlobalEnvironment getGlobalEnvironment() {
        return environment;
    }

    @Override
    public WorkMonitor getMainThreadMonitor() {
        throw new IllegalStateException("Main thread monitor not available");
    }

    @Override
    public Terminal getTerminal() {
        throw new IllegalStateException("Terminal not available");
    }

    @Override
    public FileSystem getFileSystem() {
        throw new IllegalStateException("Filesystem not available");
    }

    @Override
    public void shutdown() {
    }

    @Override
    public void reboot() {
    }

    @Override
    public void setPeripheralChangeListener(@Nullable IPeripheralChangeListener listener) {
    }

    @Nullable
    @Override
    public IPeripheral getPeripheral(ComputerSide side) {
        return null;
    }

    @Nullable
    @Override
    public String getLabel() {
        return label;
    }

    @Override
    public void setLabel(@Nullable String label) {
        this.label = label;
    }

    @Override
    public int startTimer(long ticks) {
        throw new IllegalStateException("Cannot start timers");
    }

    @Override
    public void cancelTimer(int id) {
    }

    @Override
    public MetricsObserver metrics() {
        return environment.getMetrics();
    }
}
