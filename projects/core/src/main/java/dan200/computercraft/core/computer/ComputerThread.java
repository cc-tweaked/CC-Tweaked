// SPDX-FileCopyrightText: 2017 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.core.computer;

import com.google.common.annotations.VisibleForTesting;
import com.google.errorprone.annotations.concurrent.GuardedBy;
import dan200.computercraft.core.ComputerContext;
import dan200.computercraft.core.Logging;
import dan200.computercraft.core.util.ThreadUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.Objects;
import java.util.TreeSet;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.LockSupport;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Runs all scheduled tasks for computers in a {@link ComputerContext}.
 * <p>
 * This acts as an over-complicated {@link ThreadPoolExecutor}: It creates several {@link Worker} threads which pull
 * tasks from a shared queue, executing them. It also creates a single {@link Monitor} thread, which updates computer
 * timeouts, killing workers if they have not been terminated by {@link TimeoutState#isSoftAborted()}.
 * <p>
 * Computers are executed using a priority system, with those who have spent less time executing having a higher
 * priority than those hogging the thread. This, combined with {@link TimeoutState#isPaused()} means we can reduce the
 * risk of badly behaved computers stalling execution for everyone else.
 * <p>
 * This is done using an implementation of Linux's Completely Fair Scheduler. When a computer executes, we compute what
 * share of execution time it has used (time executed/number of tasks). We then pick the computer who has the least
 * "virtual execution time" (aka {@link ComputerExecutor#virtualRuntime}).
 * <p>
 * When adding a computer to the queue, we make sure its "virtual runtime" is at least as big as the smallest runtime.
 * This means that adding computers which have slept a lot do not then have massive priority over everyone else. See
 * {@link #queue(ComputerExecutor)} for how this is implemented.
 * <p>
 * In reality, it's unlikely that more than a few computers are waiting to execute at once, so this will not have much
 * effect unless you have a computer hogging execution time. However, it is pretty effective in those situations.
 *
 * @see TimeoutState For how hard timeouts are handled.
 * @see ComputerExecutor For how computers actually do execution.
 */
@SuppressWarnings("GuardedBy") // FIXME: Hard to know what the correct thing to do is.
public final class ComputerThread {
    private static final Logger LOG = LoggerFactory.getLogger(ComputerThread.class);

    /**
     * A factory for the monitor thread. We want this a slightly higher priority than normal to ensure that the computer
     * thread is interrupted. This spends most of it its time idle, so should be safe.
     */
    private static final ThreadFactory monitorFactory = ThreadUtils.builder("Computer-Monitor")
        .setPriority((Thread.NORM_PRIORITY + Thread.MAX_PRIORITY) / 2)
        .build();

    private static final ThreadFactory workerFactory = ThreadUtils.lowPriorityFactory("Computer-Worker");

    /**
     * How often the computer thread monitor should run.
     *
     * @see Monitor
     */
    private static final long MONITOR_WAKEUP = TimeUnit.MILLISECONDS.toNanos(100);

    /**
     * The target latency between executing two tasks on a single machine.
     * <p>
     * An average tick takes 50ms, and so we ideally need to have handled a couple of events within that window in order
     * to have a perceived low latency.
     */
    private static final long DEFAULT_LATENCY = TimeUnit.MILLISECONDS.toNanos(50);

    /**
     * The minimum value that {@link #DEFAULT_LATENCY} can have when scaled.
     * <p>
     * From statistics gathered on SwitchCraft, almost all machines will execute under 15ms, 75% under 1.5ms, with the
     * mean being about 3ms. Most computers shouldn't be too impacted with having such a short period to execute in.
     */
    private static final long DEFAULT_MIN_PERIOD = TimeUnit.MILLISECONDS.toNanos(5);

    /**
     * The maximum number of tasks before we have to start scaling latency linearly.
     */
    private static final long LATENCY_MAX_TASKS = DEFAULT_LATENCY / DEFAULT_MIN_PERIOD;

    /**
     * Time difference between reporting crashed threads.
     *
     * @see Worker#reportTimeout(ComputerExecutor, long)
     */
    private static final long REPORT_DEBOUNCE = TimeUnit.SECONDS.toNanos(1);

    /**
     * Lock used for modifications to the array of current threads.
     */
    private final ReentrantLock threadLock = new ReentrantLock();

    private static final int RUNNING = 0;
    private static final int STOPPING = 1;
    private static final int CLOSED = 2;

    /**
     * Whether the computer thread system is currently running.
     */
    private final AtomicInteger state = new AtomicInteger(RUNNING);

    /**
     * The current task manager.
     */
    private @Nullable Thread monitor;

    /**
     * The array of current workers, and their owning threads.
     */
    @GuardedBy("threadLock")
    private final Worker[] workers;

    /**
     * The number of workers in {@link #workers}.
     */
    @GuardedBy("threadLock")
    private int workerCount = 0;

    private final Condition shutdown = threadLock.newCondition();

    private final long latency;
    private final long minPeriod;

    private final ReentrantLock computerLock = new ReentrantLock();
    private final Condition workerWakeup = computerLock.newCondition();
    private final Condition monitorWakeup = computerLock.newCondition();

    private final AtomicInteger idleWorkers = new AtomicInteger(0);

    /**
     * Active queues to execute.
     */
    private final TreeSet<ComputerExecutor> computerQueue = new TreeSet<>((a, b) -> {
        if (a == b) return 0; // Should never happen, but let's be consistent here

        long at = a.virtualRuntime, bt = b.virtualRuntime;
        if (at == bt) return Integer.compare(a.hashCode(), b.hashCode());
        return at < bt ? -1 : 1;
    });

    /**
     * The minimum {@link ComputerExecutor#virtualRuntime} time on the tree.
     */
    private long minimumVirtualRuntime = 0;

    public ComputerThread(int threadCount) {
        workers = new Worker[threadCount];

        // latency and minPeriod are scaled by 1 + floor(log2(threads)). We can afford to execute tasks for
        // longer when executing on more than one thread.
        var factor = 64 - Long.numberOfLeadingZeros(workers.length);
        latency = DEFAULT_LATENCY * factor;
        minPeriod = DEFAULT_MIN_PERIOD * factor;
    }

    @GuardedBy("threadLock")
    private void addWorker(int index) {
        LOG.trace("Spawning new worker {}.", index);
        (workers[index] = new Worker(index)).owner.start();
        workerCount++;
    }

    /**
     * Ensure sufficient workers are running.
     */
    @GuardedBy("computerLock")
    private void ensureRunning() {
        // Don't even enter the lock if we've a monitor and don't need to/can't spawn an additional worker.
        // We'll be holding the computer lock at this point, so there's no problems with idleWorkers being wrong.
        if (monitor != null && (idleWorkers.get() > 0 || workerCount == workers.length)) return;

        threadLock.lock();
        try {
            LOG.trace("Possibly spawning a worker or monitor.");

            if (monitor == null || !monitor.isAlive()) (monitor = monitorFactory.newThread(new Monitor())).start();
            if (idleWorkers.get() == 0 || workerCount < workers.length) {
                for (var i = 0; i < workers.length; i++) {
                    if (workers[i] == null) {
                        addWorker(i);
                        break;
                    }
                }
            }
        } finally {
            threadLock.unlock();
        }
    }

    private void advanceState(int newState) {
        while (true) {
            var current = state.get();
            if (current >= newState || state.compareAndSet(current, newState)) break;
        }
    }

    /**
     * Attempt to stop the computer thread. This interrupts each worker, and clears the task queue.
     *
     * @param timeout The maximum time to wait.
     * @param unit    The unit {@code timeout} is in.
     * @return Whether the thread was successfully shut down.
     * @throws InterruptedException If interrupted while waiting.
     */
    public boolean stop(long timeout, TimeUnit unit) throws InterruptedException {
        advanceState(STOPPING);

        // Encourage any currently running runners to terminate.
        threadLock.lock();
        try {
            for (@Nullable var worker : workers) {
                if (worker == null) continue;

                var executor = worker.currentExecutor.get();
                if (executor != null) executor.timeout.hardAbort();
            }
        } finally {
            threadLock.unlock();
        }

        // Wake all workers
        computerLock.lock();
        try {
            workerWakeup.signalAll();
        } finally {
            computerLock.unlock();
        }

        // Wait for all workers to signal they have finished.
        var timeoutNs = unit.toNanos(timeout);
        threadLock.lock();
        try {
            while (workerCount > 0) {
                if (timeoutNs <= 0) return false;
                timeoutNs = shutdown.awaitNanos(timeoutNs);
            }
        } finally {
            threadLock.unlock();
        }

        advanceState(CLOSED);

        // Signal the monitor to finish, but don't wait for it to stop.
        computerLock.lock();
        try {
            monitorWakeup.signal();
        } finally {
            computerLock.unlock();
        }

        return true;
    }

    /**
     * Mark a computer as having work, enqueuing it on the thread.
     * <p>
     * You must be holding {@link ComputerExecutor}'s {@code queueLock} when calling this method - it should only
     * be called from {@code enqueue}.
     *
     * @param executor The computer to execute work on.
     */
    void queue(ComputerExecutor executor) {
        computerLock.lock();
        try {
            if (state.get() != RUNNING) throw new IllegalStateException("ComputerThread is no longer running");

            // Ensure we've got a worker running.
            ensureRunning();

            if (executor.onComputerQueue) throw new IllegalStateException("Cannot queue already queued executor");
            executor.onComputerQueue = true;

            updateRuntimes(null);

            // We're not currently on the queue, so update its current execution time to
            // ensure its at least as high as the minimum.
            var newRuntime = minimumVirtualRuntime;

            if (executor.virtualRuntime == 0) {
                // Slow down new computers a little bit.
                newRuntime += scaledPeriod();
            } else {
                // Give a small boost to computers which have slept a little.
                newRuntime -= latency / 2;
            }

            executor.virtualRuntime = Math.max(newRuntime, executor.virtualRuntime);

            var wasBusy = isBusy();
            // Add to the queue, and signal the workers.
            computerQueue.add(executor);
            workerWakeup.signal();

            // If we've transitioned into a busy state, notify the monitor. This will cause it to sleep for scaledPeriod
            // instead of the longer wakeup duration.
            if (!wasBusy && isBusy()) monitorWakeup.signal();
        } finally {
            computerLock.unlock();
        }
    }


    /**
     * Update the {@link ComputerExecutor#virtualRuntime}s of all running tasks, and then update the
     * {@link #minimumVirtualRuntime} based on the current tasks.
     * <p>
     * This is called before queueing tasks, to ensure that {@link #minimumVirtualRuntime} is up-to-date.
     *
     * @param current The machine which we updating runtimes from.
     */
    private void updateRuntimes(@Nullable ComputerExecutor current) {
        var minRuntime = Long.MAX_VALUE;

        // If we've a task on the queue, use that as our base time.
        if (!computerQueue.isEmpty()) minRuntime = computerQueue.first().virtualRuntime;

        // Update all the currently executing tasks
        var now = System.nanoTime();
        var tasks = 1 + computerQueue.size();
        for (@Nullable var runner : workers) {
            if (runner == null) continue;
            var executor = runner.currentExecutor.get();
            if (executor == null) continue;

            // We do two things here: first we update the task's virtual runtime based on when we
            // last checked, and then we check the minimum.
            minRuntime = Math.min(minRuntime, executor.virtualRuntime += (now - executor.vRuntimeStart) / tasks);
            executor.vRuntimeStart = now;
        }

        // And update the most recently executed one (if set).
        if (current != null) {
            minRuntime = Math.min(minRuntime, current.virtualRuntime += (now - current.vRuntimeStart) / tasks);
        }

        if (minRuntime > minimumVirtualRuntime && minRuntime < Long.MAX_VALUE) {
            minimumVirtualRuntime = minRuntime;
        }
    }

    /**
     * Ensure the "currently working" state of the executor is reset, the timings are updated, and then requeue the
     * executor if needed.
     *
     * @param runner   The runner this task was on.
     * @param executor The executor to requeue
     */
    private void afterWork(Worker runner, ComputerExecutor executor) {
        // Clear the executor's thread.
        var currentThread = executor.executingThread.getAndSet(null);
        if (currentThread != runner.owner) {

            LOG.error(
                "Expected computer #{} to be running on {}, but already running on {}. This is a SERIOUS bug, please report with your debug.log.",
                executor.getComputer().getID(),
                runner.owner.getName(),
                currentThread == null ? "nothing" : currentThread.getName()
            );
        }

        computerLock.lock();
        try {
            updateRuntimes(executor);

            // If we've no more tasks, just return.
            if (!executor.afterWork() || state.get() != RUNNING) return;

            // Otherwise, add to the queue, and signal any waiting workers.
            computerQueue.add(executor);
            workerWakeup.signal();
        } finally {
            computerLock.unlock();
        }
    }

    /**
     * The scaled period for a single task.
     *
     * @return The scaled period for the task
     * @see #DEFAULT_LATENCY
     * @see #DEFAULT_MIN_PERIOD
     * @see #LATENCY_MAX_TASKS
     */
    long scaledPeriod() {
        // FIXME: We access this on other threads (in TimeoutState), so their reads won't be consistent. This isn't
        //  "critical" behaviour, so not clear if it matters too much.

        // +1 to include the current task
        var count = 1 + computerQueue.size();
        return count < LATENCY_MAX_TASKS ? latency / count : minPeriod;
    }

    /**
     * Determine if the thread has computers queued up.
     *
     * @return If we have work queued up.
     */
    @VisibleForTesting
    public boolean hasPendingWork() {
        // FIXME: See comment in scaledPeriod. Again, we access this in multiple threads but not clear if it matters!
        return !computerQueue.isEmpty();
    }

    /**
     * Check if we have more work queued than we have capacity for. Effectively a more fine-grained version of
     * {@link #hasPendingWork()}.
     *
     * @return If the computer threads are busy.
     */
    @GuardedBy("computerLock")
    private boolean isBusy() {
        return computerQueue.size() > idleWorkers.get();
    }

    private void workerFinished(Worker worker) {
        // We should only shut down a worker once! This should only happen if we fail to abort a worker and then the
        // worker finishes normally.
        if (!worker.running.getAndSet(false)) return;

        LOG.trace("Worker {} finished.", worker.index);

        var executor = worker.currentExecutor.getAndSet(null);
        if (executor != null) executor.afterWork();

        threadLock.lock();
        try {
            workerCount--;

            if (workers[worker.index] != worker) {
                LOG.error("Worker {} closed, but new runner has been spawned.", worker.index);
            } else if (state.get() == RUNNING || (state.get() == STOPPING && hasPendingWork())) {
                addWorker(worker.index);
                workerCount++;
            } else {
                workers[worker.index] = null;
            }
        } finally {
            threadLock.unlock();
        }
    }

    /**
     * Observes all currently active {@link Worker}s and terminates their tasks once they have exceeded the hard
     * abort limit.
     *
     * @see TimeoutState
     */
    private final class Monitor implements Runnable {
        @Override
        public void run() {
            LOG.trace("Monitor starting.");
            try {
                runImpl();
            } finally {
                LOG.trace("Monitor shutting down. Current state is {}.", state.get());
            }
        }

        private void runImpl() {
            while (state.get() < CLOSED) {
                computerLock.lock();
                try {
                    // If we've got more work than we have capacity for it, then we'll need to pause a task soon, so
                    // sleep for a single pause duration. Otherwise we only need to wake up to set the soft/hard abort
                    // flags, which are far less granular.
                    monitorWakeup.awaitNanos(isBusy() ? scaledPeriod() : MONITOR_WAKEUP);
                } catch (InterruptedException e) {
                    LOG.error("Monitor thread interrupted. Computers may behave very badly!", e);
                    break;
                } finally {
                    computerLock.unlock();
                }

                checkRunners();
            }
        }

        private void checkRunners() {
            for (@Nullable var runner : workers) {
                if (runner == null) continue;

                // If the worker has no work, skip
                var executor = runner.currentExecutor.get();
                if (executor == null) continue;

                // Refresh the timeout state. Will set the pause/soft timeout flags as appropriate.
                executor.timeout.refresh();

                // If we're still within normal execution times (TIMEOUT) or soft abort (ABORT_TIMEOUT),
                // then we can let the Lua machine do its work.
                var afterStart = executor.timeout.nanoCumulative();
                var afterHardAbort = afterStart - TimeoutState.TIMEOUT - TimeoutState.ABORT_TIMEOUT;
                if (afterHardAbort < 0) continue;

                // Set the hard abort flag.
                executor.timeout.hardAbort();
                executor.abort();

                if (afterHardAbort >= TimeoutState.ABORT_TIMEOUT * 2) {
                    // If we've hard aborted and interrupted, and we're still not dead, then mark the worker
                    // as dead, finish off the task, and spawn a new runner.
                    runner.reportTimeout(executor, afterStart);
                    runner.owner.interrupt();

                    workerFinished(runner);
                } else if (afterHardAbort >= TimeoutState.ABORT_TIMEOUT) {
                    // If we've hard aborted but we're still not dead, dump the stack trace and interrupt
                    // the task.
                    runner.reportTimeout(executor, afterStart);
                    runner.owner.interrupt();
                }
            }
        }
    }

    /**
     * Pulls tasks from the {@link #computerQueue} queue and runs them.
     * <p>
     * This is responsible for running the {@link ComputerExecutor#work()}, {@link ComputerExecutor#beforeWork()} and
     * {@link ComputerExecutor#afterWork()} functions. Everything else is either handled by the executor, timeout
     * state or monitor.
     */
    private final class Worker implements Runnable {
        /**
         * The index into the {@link #workers} array.
         */
        final int index;

        /**
         * The thread this runner runs on.
         */
        final Thread owner;

        /**
         * Whether this runner is currently executing. This may be set to false when this worker terminates, or when
         * we try to abandon a worker in the monitor
         *
         * @see #workerFinished(Worker)
         */
        final AtomicBoolean running = new AtomicBoolean(true);

        /**
         * The computer we're currently running.
         */
        final AtomicReference<ComputerExecutor> currentExecutor = new AtomicReference<>(null);

        /**
         * The last time we reported a stack trace, used to avoid spamming the logs.
         */
        AtomicLong lastReport = new AtomicLong(Long.MIN_VALUE);

        Worker(int index) {
            this.index = index;
            owner = workerFactory.newThread(this);
        }

        @Override
        public void run() {
            try {
                runImpl();
            } finally {
                workerFinished(this);
            }
        }

        private void runImpl() {
            tasks:
            while (running.get()) {
                // Wait for an active queue to execute
                ComputerExecutor executor;
                computerLock.lock();
                try {
                    idleWorkers.getAndIncrement();
                    while ((executor = computerQueue.pollFirst()) == null) {
                        if (state.get() >= STOPPING) return;

                        // We should never interrupt() the worker, so this should be fine.
                        workerWakeup.awaitUninterruptibly();
                    }
                } finally {
                    idleWorkers.getAndDecrement();
                    computerLock.unlock();
                }

                // If we're trying to executing some task on this computer while someone else is doing work, something
                // is seriously wrong.
                while (!executor.executingThread.compareAndSet(null, owner)) {
                    var existing = executor.executingThread.get();
                    if (existing != null) {
                        LOG.error(
                            "Trying to run computer #{} on thread {}, but already running on {}. This is a SERIOUS bug, please report with your debug.log.",
                            executor.getComputer().getID(), owner.getName(), existing.getName()
                        );
                        continue tasks;
                    }
                }

                // If we're stopping, the only thing this executor should be doing is shutting down.
                if (state.get() >= STOPPING) executor.queueStop(false, true);

                // Reset the timers
                executor.beforeWork();

                // And then set the current executor. It's important to do it afterwards, as otherwise we introduce
                // race conditions with the monitor.
                currentExecutor.set(executor);

                // Execute the task
                try {
                    executor.work();
                } catch (Exception | LinkageError | VirtualMachineError e) {
                    LOG.error("Error running task on computer #" + executor.getComputer().getID(), e);
                    // Tear down the computer immediately. There's no guarantee it's well-behaved from now on.
                    executor.fastFail();
                } finally {
                    var thisExecutor = currentExecutor.getAndSet(null);
                    if (thisExecutor != null) afterWork(this, executor);
                }
            }
        }

        private void reportTimeout(ComputerExecutor executor, long time) {
            if (!LOG.isErrorEnabled(Logging.COMPUTER_ERROR)) return;

            // Attempt to debounce stack trace reporting, limiting ourselves to one every second. There's no need to be
            // ultra-precise in our atomics, as long as one of them wins!
            var now = System.nanoTime();
            var then = lastReport.get();
            if (then != Long.MIN_VALUE && now - then - REPORT_DEBOUNCE <= 0) return;
            if (!lastReport.compareAndSet(then, now)) return;

            var owner = Objects.requireNonNull(this.owner);

            var builder = new StringBuilder()
                .append("Terminating computer #").append(executor.getComputer().getID())
                .append(" due to timeout (running for ").append(time * 1e-9)
                .append(" seconds). This is NOT a bug, but may mean a computer is misbehaving.\n")
                .append("Thread ")
                .append(owner.getName())
                .append(" is currently ")
                .append(owner.getState())
                .append('\n');
            var blocking = LockSupport.getBlocker(owner);
            if (blocking != null) builder.append("  on ").append(blocking).append('\n');

            for (var element : owner.getStackTrace()) {
                builder.append("  at ").append(element).append('\n');
            }

            executor.printState(builder);

            LOG.warn(builder.toString());
        }
    }
}
