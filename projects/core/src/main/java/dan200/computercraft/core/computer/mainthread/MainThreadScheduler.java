/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.core.computer.mainthread;

import dan200.computercraft.api.peripheral.IWorkMonitor;
import dan200.computercraft.core.metrics.MetricsObserver;

import java.util.OptionalLong;

/**
 * A {@link MainThreadScheduler} is responsible for running work on the main thread, for instance the server thread in
 * Minecraft.
 *
 * @see MainThread is the default implementation
 */
public interface MainThreadScheduler {
    /**
     * Create an executor for a computer. This should only be called once for a single computer.
     *
     * @param observer A sink for metrics, used to monitor task timings.
     * @return The executor for this computer.
     */
    Executor createExecutor(MetricsObserver observer);

    /**
     * An {@link Executor} is responsible for managing scheduled tasks for a single computer.
     */
    interface Executor extends IWorkMonitor {
        /**
         * Schedule a task to be run on the main thread. This can be called from any thread.
         *
         * @param task The task to schedule.
         * @return The task ID if the task could be scheduled, or {@link OptionalLong#empty()} if the task failed to
         * be scheduled.
         */
        boolean enqueue(Runnable task);
    }
}
