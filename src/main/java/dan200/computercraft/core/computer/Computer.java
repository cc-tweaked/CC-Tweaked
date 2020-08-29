/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.core.computer;

import java.util.concurrent.atomic.AtomicBoolean;

import com.google.common.base.Objects;
import dan200.computercraft.api.lua.ILuaAPI;
import dan200.computercraft.api.peripheral.IWorkMonitor;
import dan200.computercraft.core.apis.IAPIEnvironment;
import dan200.computercraft.core.filesystem.FileSystem;
import dan200.computercraft.core.terminal.Terminal;

/**
 * Represents a computer which may exist in-world or elsewhere.
 *
 * Note, this class has several (read: far, far too many) responsibilities, so can get a little unwieldy at times.
 *
 * <ul>
 * <li>Updates the {@link Environment}.</li>
 * <li>Keeps track of whether the computer is on and blinking.</li>
 * <li>Monitors whether the computer's visible state (redstone, on/off/blinking) has changed.</li>
 * <li>Passes commands and events to the {@link ComputerExecutor}.</li>
 * <li>Passes main thread tasks to the {@link MainThreadExecutor}.</li>
 * </ul>
 */
public class Computer {
    private static final int START_DELAY = 50;
    // Read-only fields about the computer
    private final IComputerEnvironment m_environment;
    private final Terminal m_terminal;
    private final ComputerExecutor executor;
    private final MainThreadExecutor serverExecutor;
    private final Environment internalEnvironment = new Environment(this);
    // Various properties of the computer
    private int m_id;
    private String m_label = null;
    // Additional state about the computer and its environment.
    private boolean m_blinking = false;
    private AtomicBoolean externalOutputChanged = new AtomicBoolean();

    private boolean startRequested;
    private int m_ticksSinceStart = -1;

    public Computer(IComputerEnvironment environment, Terminal terminal, int id) {
        this.m_id = id;
        this.m_environment = environment;
        this.m_terminal = terminal;

        this.executor = new ComputerExecutor(this);
        this.serverExecutor = new MainThreadExecutor(this);
    }

    IComputerEnvironment getComputerEnvironment() {
        return this.m_environment;
    }

    FileSystem getFileSystem() {
        return this.executor.getFileSystem();
    }

    Terminal getTerminal() {
        return this.m_terminal;
    }

    public Environment getEnvironment() {
        return this.internalEnvironment;
    }

    public IAPIEnvironment getAPIEnvironment() {
        return this.internalEnvironment;
    }

    public void turnOn() {
        this.startRequested = true;
    }

    public void shutdown() {
        this.executor.queueStop(false, false);
    }

    public void reboot() {
        this.executor.queueStop(true, false);
    }

    public void unload() {
        this.executor.queueStop(false, true);
    }

    public void queueEvent(String event, Object[] args) {
        this.executor.queueEvent(event, args);
    }

    /**
     * Queue a task to be run on the main thread, using {@link MainThread}.
     *
     * @param runnable The task to run
     * @return If the task was successfully queued (namely, whether there is space on it).
     */
    public boolean queueMainThread(Runnable runnable) {
        return this.serverExecutor.enqueue(runnable);
    }

    public IWorkMonitor getMainThreadMonitor() {
        return this.serverExecutor;
    }

    public int getID() {
        return this.m_id;
    }

    public void setID(int id) {
        this.m_id = id;
    }

    public int assignID() {
        if (this.m_id < 0) {
            this.m_id = this.m_environment.assignNewID();
        }
        return this.m_id;
    }

    public String getLabel() {
        return this.m_label;
    }

    public void setLabel(String label) {
        if (!Objects.equal(label, this.m_label)) {
            this.m_label = label;
            this.externalOutputChanged.set(true);
        }
    }

    public void tick() {
        // We keep track of the number of ticks since the last start, only
        if (this.m_ticksSinceStart >= 0 && this.m_ticksSinceStart <= START_DELAY) {
            this.m_ticksSinceStart++;
        }

        if (this.startRequested && (this.m_ticksSinceStart < 0 || this.m_ticksSinceStart > START_DELAY)) {
            this.startRequested = false;
            if (!this.executor.isOn()) {
                this.m_ticksSinceStart = 0;
                this.executor.queueStart();
            }
        }

        this.executor.tick();

        // Update the environment's internal state.
        this.internalEnvironment.update();

        // Propagate the environment's output to the world.
        if (this.internalEnvironment.updateOutput()) {
            this.externalOutputChanged.set(true);
        }

        // Set output changed if the terminal has changed from blinking to not
        boolean blinking = this.m_terminal.getCursorBlink() && this.m_terminal.getCursorX() >= 0 && this.m_terminal.getCursorX() < this.m_terminal.getWidth() && this.m_terminal.getCursorY() >= 0 && this.m_terminal.getCursorY() < this.m_terminal.getHeight();
        if (blinking != this.m_blinking) {
            this.m_blinking = blinking;
            this.externalOutputChanged.set(true);
        }
    }

    void markChanged() {
        this.externalOutputChanged.set(true);
    }

    public boolean pollAndResetChanged() {
        return this.externalOutputChanged.getAndSet(false);
    }

    public boolean isBlinking() {
        return this.isOn() && this.m_blinking;
    }

    public boolean isOn() {
        return this.executor.isOn();
    }

    public void addApi(ILuaAPI api) {
        this.executor.addApi(api);
    }
}
