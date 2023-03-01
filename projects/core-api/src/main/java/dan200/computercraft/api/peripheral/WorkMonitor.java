// SPDX-FileCopyrightText: 2019 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.api.peripheral;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * Monitors "work" associated with a computer, keeping track of how much a computer has done, and ensuring every
 * computer receives a fair share of any processing time.
 * <p>
 * This is primarily intended for work done by peripherals on the main thread (such as on a tile entity's tick), but
 * could be used for other purposes (such as complex computations done on another thread).
 * <p>
 * Before running a task, one should call {@link #canWork()} to determine if the computer is currently allowed to
 * execute work. If that returns true, you should execute the task and use {@link #trackWork(long, TimeUnit)} to inform
 * the monitor how long that task took.
 * <p>
 * Alternatively, use {@link #runWork(Runnable)} to run and keep track of work.
 *
 * @see IComputerAccess#getMainThreadMonitor()
 */
public interface WorkMonitor {
    /**
     * If the owning computer is currently allowed to execute work.
     *
     * @return If we can execute work right now.
     */
    boolean canWork();

    /**
     * If the owning computer is currently allowed to execute work, and has ample time to do so.
     * <p>
     * This is effectively a more restrictive form of {@link #canWork()}. One should use that in order to determine if
     * you may do an initial piece of work, and shouldWork to determine if any additional task may be performed.
     *
     * @return If we should execute work right now.
     */
    boolean shouldWork();

    /**
     * Inform the monitor how long some piece of work took to execute.
     *
     * @param time The time some task took to run
     * @param unit The unit that {@code time} was measured in.
     */
    void trackWork(long time, TimeUnit unit);

    /**
     * Run a task if possible, and inform the monitor of how long it took.
     *
     * @param runnable The task to run.
     * @return If the task was actually run (namely, {@link #canWork()} returned {@code true}).
     */
    default boolean runWork(Runnable runnable) {
        Objects.requireNonNull(runnable, "runnable should not be null");
        if (!canWork()) return false;

        var start = System.nanoTime();
        try {
            runnable.run();
        } finally {
            trackWork(System.nanoTime() - start, TimeUnit.NANOSECONDS);
        }

        return true;
    }
}
