// SPDX-FileCopyrightText: 2017 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.core.computer.mainthread;

import dan200.computercraft.core.metrics.MetricsObserver;

import java.util.HashSet;
import java.util.TreeSet;

/**
 * Runs tasks on the main (server) thread, ticks {@link MainThreadExecutor}s, and limits how much time is used this
 * tick.
 * <p>
 * Similar to {@link MainThreadExecutor}, the {@link MainThread} can be in one of three states: cool, hot and cooling.
 * However, the implementation here is a little different:
 * <p>
 * {@link MainThread} starts cool, and runs as many tasks as it can in the current {@link #budget}ns. Any external tasks
 * (those run by tile entities, etc...) will also consume the budget
 * <p>
 * Next tick, we add {@link MainThreadConfig#maxGlobalTime()} to our budget (clamp it to that value too). If we're still
 * over budget, then we should not execute <em>any</em> work (either as part of {@link MainThread} or externally).
 */
public final class MainThread implements MainThreadScheduler {
    /**
     * The queue of {@link MainThreadExecutor}s with tasks to perform.
     */
    private final TreeSet<MainThreadExecutor> executors = new TreeSet<>((a, b) -> {
        if (a == b) return 0; // Should never happen, but let's be consistent here

        long at = a.virtualTime, bt = b.virtualTime;
        if (at == bt) return Integer.compare(a.hashCode(), b.hashCode());
        return at < bt ? -1 : 1;
    });

    final MainThreadConfig config;

    /**
     * The set of executors which went over budget in a previous tick, and are waiting for their time to run down.
     *
     * @see MainThreadExecutor#tickCooling()
     * @see #cooling(MainThreadExecutor)
     */
    private final HashSet<MainThreadExecutor> cooling = new HashSet<>();

    /**
     * The current tick number. This is used by {@link MainThreadExecutor} to determine when to reset its own time
     * counter.
     *
     * @see #currentTick()
     */
    private int currentTick;

    /**
     * The remaining budgeted time for this tick. This may be negative, in the case that we've gone over budget.
     */
    private long budget;

    /**
     * Whether we should be executing any work this tick.
     * <p>
     * This is true iff {@code MAX_TICK_TIME - currentTime} was true <em>at the beginning of the tick</em>.
     */
    private boolean canExecute = true;

    private long minimumTime = 0;

    public MainThread() {
        this(MainThreadConfig.DEFAULT);
    }

    public MainThread(MainThreadConfig config) {
        this.config = config;
    }

    void queue(MainThreadExecutor executor) {
        synchronized (executors) {
            if (executor.onQueue) throw new IllegalStateException("Cannot queue already queued executor");
            executor.onQueue = true;
            executor.updateTime();

            // We're not currently on the queue, so update its current execution time to
            // ensure it's at least as high as the minimum.
            var newRuntime = minimumTime;

            // Slow down new computers a little bit.
            if (executor.virtualTime == 0) newRuntime += config.maxComputerTime();

            executor.virtualTime = Math.max(newRuntime, executor.virtualTime);

            executors.add(executor);
        }
    }

    void cooling(MainThreadExecutor executor) {
        cooling.add(executor);
    }

    void consumeTime(long time) {
        budget -= time;
    }

    boolean canExecute() {
        return canExecute;
    }

    int currentTick() {
        return currentTick;
    }

    public void tick() {
        // Move onto the next tick and cool down the global executor. We're allowed to execute if we have _any_ time
        // allocated for this tick. This means we'll stick much closer to doing MAX_TICK_TIME work every tick.
        //
        // Of course, we'll go over the MAX_TICK_TIME most of the time, but eventually that overrun will accumulate
        // and we'll skip a whole tick - bringing the average back down again.
        currentTick++;
        var maxGlobal = config.maxGlobalTime();
        budget = Math.min(budget + maxGlobal, maxGlobal);
        canExecute = budget > 0;

        // Cool down any warm computers.
        cooling.removeIf(MainThreadExecutor::tickCooling);

        if (!canExecute) return;

        // Run until we meet the deadline.
        var start = System.nanoTime();
        var deadline = start + budget;
        while (true) {
            MainThreadExecutor executor;
            synchronized (executors) {
                executor = executors.pollFirst();
            }
            if (executor == null) break;

            var taskStart = System.nanoTime();
            executor.execute();

            var taskStop = System.nanoTime();
            synchronized (executors) {
                if (executor.afterExecute(taskStop - taskStart)) executors.add(executor);

                // Compute the new minimum time (including the next task on the queue too). Note that this may also include
                // time spent in external tasks.
                var newMinimum = executor.virtualTime;
                if (!executors.isEmpty()) {
                    var next = executors.first();
                    if (next.virtualTime < newMinimum) newMinimum = next.virtualTime;
                }
                minimumTime = Math.max(minimumTime, newMinimum);
            }

            if (taskStop >= deadline) break;
        }

        consumeTime(System.nanoTime() - start);
    }

    @Override
    public Executor createExecutor(MetricsObserver metrics) {
        return new MainThreadExecutor(metrics, this);
    }
}
