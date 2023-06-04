// SPDX-FileCopyrightText: 2019 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.core.computer.mainthread;

import dan200.computercraft.api.peripheral.WorkMonitor;
import dan200.computercraft.core.computer.Computer;
import dan200.computercraft.core.metrics.Metrics;
import dan200.computercraft.core.metrics.MetricsObserver;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.TimeUnit;

/**
 * Keeps track of tasks that a {@link Computer} should run on the main thread and how long that has been spent executing
 * them.
 * <p>
 * This provides rate-limiting mechanism for tasks enqueued with {@link Computer#queueMainThread(Runnable)}, but also
 * those run elsewhere (such as during the turtle's tick). In order to handle this, the executor goes through three
 * stages:
 * <p>
 * When {@link State#COOL}, the computer is allocated {@link MainThreadConfig#maxComputerTime()}ns to execute any work
 * this tick. At the beginning of the tick, we execute as many {@link MainThread} tasks as possible, until our
 * time-frame or the global time frame has expired.
 * <p>
 * Then, when other objects (such as block entities or entities) are ticked, we update how much time we've used via
 * {@link WorkMonitor#trackWork(long, TimeUnit)}.
 * <p>
 * Now, if anywhere during this period, we use more than our allocated time slice, the executor is marked as
 * {@link State#HOT}. This means it will no longer be able to execute {@link MainThread} tasks (though will still
 * execute tile entity tasks, in order to prevent the main thread from exhausting work every tick).
 * <p>
 * At the beginning of the next tick, we increment the budget e by {@link MainThreadConfig#maxComputerTime()} and any
 * {@link State#HOT} executors are marked as {@link State#COOLING}. They will remain cooling until their budget is fully
 * replenished (is equal to {@link MainThreadConfig#maxComputerTime()}). Note, this is different to {@link MainThread},
 * which allows running when it has any budget left. When cooling, <em>no</em> tasks are executed - be they on the tile
 * entity or main thread.
 * <p>
 * This mechanism means that, on average, computers will use at most {@link MainThreadConfig#maxComputerTime()}ns per
 * second, but one task source will not prevent others from executing.
 *
 * @see MainThread
 * @see WorkMonitor
 * @see Computer#getMainThreadMonitor()
 * @see Computer#queueMainThread(Runnable)
 */
final class MainThreadExecutor implements MainThreadScheduler.Executor {
    /**
     * The maximum number of {@link MainThread} tasks allowed on the queue.
     */
    private static final int MAX_TASKS = 5000;

    private final MetricsObserver metrics;

    /**
     * A lock used for any changes to {@link #tasks}, or {@link #onQueue}. This will be
     * used on the main thread, so locks should be kept as brief as possible.
     */
    private final Object queueLock = new Object();

    /**
     * The queue of tasks which should be executed.
     *
     * @see #queueLock
     */
    private final Queue<Runnable> tasks = new ArrayDeque<>(4);

    /**
     * Determines if this executor is currently present on the queue.
     * <p>
     * This should be true iff {@link #tasks} is non-empty.
     *
     * @see #queueLock
     * @see #enqueue(Runnable)
     * @see #afterExecute(long)
     */
    volatile boolean onQueue;

    /**
     * The remaining budgeted time for this tick. This may be negative, in the case that we've gone over budget.
     *
     * @see #tickCooling()
     * @see #consumeTime(long)
     */
    private long budget = 0;

    /**
     * The last tick that {@link #budget} was updated.
     *
     * @see #tickCooling()
     * @see #consumeTime(long)
     */
    private int currentTick = -1;

    /**
     * The current state of this executor.
     *
     * @see #canWork()
     */
    private State state = State.COOL;

    private long pendingTime;

    long virtualTime;

    private final MainThread scheduler;

    MainThreadExecutor(MetricsObserver metrics, MainThread scheduler) {
        this.metrics = metrics;
        this.scheduler = scheduler;
    }

    /**
     * Push a task onto this executor's queue, pushing it onto the {@link MainThread} if needed.
     *
     * @param runnable The task to run on the main thread.
     * @return Whether this task was enqueued (namely, was there space).
     */
    @Override
    public boolean enqueue(Runnable runnable) {
        synchronized (queueLock) {
            if (tasks.size() >= MAX_TASKS || !tasks.offer(runnable)) return false;
            if (!onQueue && state == State.COOL) scheduler.queue(this);
            return true;
        }
    }

    void execute() {
        if (state != State.COOL) return;

        Runnable task;
        synchronized (queueLock) {
            task = tasks.poll();
        }

        if (task != null) task.run();
    }

    /**
     * Update the time taken to run an {@link #enqueue(Runnable)} task.
     *
     * @param time The time some task took to run.
     * @return Whether this should be added back to the queue.
     */
    boolean afterExecute(long time) {
        consumeTime(time);

        synchronized (queueLock) {
            virtualTime += time;
            updateTime();
            if (state != State.COOL || tasks.isEmpty()) return onQueue = false;
            return true;
        }
    }

    /**
     * Whether we should execute "external" tasks (ones not part of {@link #tasks}).
     *
     * @return Whether we can execute external tasks.
     */
    @Override
    public boolean canWork() {
        return state != State.COOLING && scheduler.canExecute();
    }

    @Override
    public boolean shouldWork() {
        return state == State.COOL && scheduler.canExecute();
    }

    @Override
    public void trackWork(long time, TimeUnit unit) {
        var nanoTime = unit.toNanos(time);
        synchronized (queueLock) {
            pendingTime += nanoTime;
        }

        consumeTime(nanoTime);
        scheduler.consumeTime(nanoTime);
    }

    private void consumeTime(long time) {
        metrics.observe(Metrics.SERVER_TASKS, time);

        // Reset the budget if moving onto a new tick. We know this is safe, as this will only have happened if
        // #tickCooling() isn't called, and so we didn't overrun the previous tick.
        if (currentTick != scheduler.currentTick()) {
            currentTick = scheduler.currentTick();
            budget = scheduler.config.maxComputerTime();
        }

        budget -= time;

        // If we've gone over our limit, mark us as having to cool down.
        if (budget < 0 && state == State.COOL) {
            state = State.HOT;
            scheduler.cooling(this);
        }
    }

    /**
     * Move this executor forward one tick, replenishing the budget by {@link MainThreadConfig#maxComputerTime()}.
     *
     * @return Whether this executor has cooled down, and so is safe to run again.
     */
    boolean tickCooling() {
        state = State.COOLING;
        currentTick = scheduler.currentTick();
        var maxTime = scheduler.config.maxComputerTime();
        budget = Math.min(budget + maxTime, maxTime);
        if (budget < maxTime) return false;

        state = State.COOL;
        synchronized (queueLock) {
            if (!tasks.isEmpty() && !onQueue) scheduler.queue(this);
        }
        return true;
    }

    void updateTime() {
        virtualTime += pendingTime;
        pendingTime = 0;
    }

    private enum State {
        COOL,
        HOT,
        COOLING,
    }
}
