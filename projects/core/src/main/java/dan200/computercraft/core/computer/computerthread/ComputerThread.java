// SPDX-FileCopyrightText: 2017 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.core.computer.computerthread;

import com.google.common.annotations.VisibleForTesting;
import com.google.errorprone.annotations.Keep;
import dan200.computercraft.core.ComputerContext;
import dan200.computercraft.core.Logging;
import dan200.computercraft.core.computer.TimeoutState;
import dan200.computercraft.core.metrics.Metrics;
import dan200.computercraft.core.metrics.MetricsObserver;
import dan200.computercraft.core.metrics.ThreadAllocations;
import dan200.computercraft.core.util.ThreadUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.annotation.concurrent.GuardedBy;
import java.util.Arrays;
import java.util.Objects;
import java.util.TreeSet;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.*;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.LockSupport;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Runs all scheduled tasks for computers in a {@link ComputerContext}.
 * <p>
 * This acts as an over-complicated {@link ThreadPoolExecutor}: It creates several {@linkplain WorkerThread worker
 * threads} which pull tasks from a shared queue, executing them. It also creates a single {@link Monitor} thread, which
 * updates computer timeouts, killing workers if they have not been terminated by {@link TimeoutState#isSoftAborted()}.
 * <p>
 * Computers are executed using a priority system, with those who have spent less time executing having a higher
 * priority than those hogging the thread. This, combined with {@link TimeoutState#isPaused()} means we can reduce the
 * risk of badly behaved computers stalling execution for everyone else.
 * <p>
 * This is done using an implementation of Linux's Completely Fair Scheduler. When a computer executes, we compute what
 * share of execution time it has used (time executed/number of tasks). We then pick the computer who has the least
 * "virtual execution time" (aka {@link ExecutorImpl#virtualRuntime}).
 * <p>
 * When adding a computer to the queue, we make sure its "virtual runtime" is at least as big as the smallest runtime.
 * This means that adding computers which have slept a lot do not then have massive priority over everyone else. See
 * {@link #queue(ExecutorImpl)} for how this is implemented.
 * <p>
 * In reality, it's unlikely that more than a few computers are waiting to execute at once, so this will not have much
 * effect unless you have a computer hogging execution time. However, it is pretty effective in those situations.
 */
public final class ComputerThread implements ComputerScheduler {
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
     * @see WorkerThread#reportTimeout(ExecutorImpl, long)
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
    private final WorkerThread[] workers;

    /**
     * The number of workers in {@link #workers}.
     */
    @GuardedBy("threadLock")
    private int workerCount = 0;

    private final Condition shutdown = threadLock.newCondition();

    private final long latency;
    private final long minPeriod;

    private final ReentrantLock computerLock = new ReentrantLock();
    private final @GuardedBy("computerLock") Condition workerWakeup = computerLock.newCondition();
    private final @GuardedBy("computerLock") Condition monitorWakeup = computerLock.newCondition();

    private final AtomicInteger idleWorkers = new AtomicInteger(0);

    /**
     * Active queues to execute.
     */
    @GuardedBy("computerLock")
    private final TreeSet<ExecutorImpl> computerQueue = new TreeSet<>(ComputerThread::compareExecutors);

    @SuppressWarnings("GuardedBy")
    private static int compareExecutors(ExecutorImpl a, ExecutorImpl b) {
        if (a == b) return 0; // Should never happen, but let's be consistent here

        long at = a.virtualRuntime, bt = b.virtualRuntime;
        if (at == bt) return Integer.compare(a.hashCode(), b.hashCode());
        return at < bt ? -1 : 1;
    }

    /**
     * The minimum {@link ExecutorImpl#virtualRuntime} time on the tree.
     */
    private long minimumVirtualRuntime = 0;

    public ComputerThread(int threadCount) {
        workers = new WorkerThread[threadCount];

        // latency and minPeriod are scaled by 1 + floor(log2(threads)). We can afford to execute tasks for
        // longer when executing on more than one thread.
        var factor = 64 - Long.numberOfLeadingZeros(workers.length);
        latency = DEFAULT_LATENCY * factor;
        minPeriod = DEFAULT_MIN_PERIOD * factor;
    }

    @Override
    public Executor createExecutor(ComputerScheduler.Worker worker, MetricsObserver metrics) {
        return new ExecutorImpl(worker, metrics);
    }

    @GuardedBy("threadLock")
    private void addWorker(int index) {
        LOG.trace("Spawning new worker {}.", index);
        (workers[index] = new WorkerThread(index)).owner.start();
        workerCount++;
    }

    @SuppressWarnings("GuardedBy")
    private int workerCount() {
        return workerCount;
    }

    @SuppressWarnings("GuardedBy")
    private WorkerThread[] workersReadOnly() {
        return workers;
    }

    /**
     * Ensure sufficient workers are running.
     */
    @GuardedBy("computerLock")
    private void ensureRunning() {
        // Don't even enter the lock if we've a monitor and don't need to/can't spawn an additional worker.
        // We'll be holding the computer lock at this point, so there's no problems with idleWorkers being wrong.
        if (monitor != null && (idleWorkers.get() > 0 || workerCount() == workersReadOnly().length)) return;

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
    @Override
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
     * You must be holding {@link ExecutorImpl}'s {@code queueLock} when calling this method - it should only
     * be called from {@code enqueue}.
     *
     * @param executor The computer to execute work on.
     */
    void queue(ExecutorImpl executor) {
        computerLock.lock();
        try {
            if (state.get() != RUNNING) throw new IllegalStateException("ComputerThread is no longer running");

            // Ensure we've got a worker running.
            ensureRunning();

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
     * Update the {@link ExecutorImpl#virtualRuntime}s of all running tasks, and then update the
     * {@link #minimumVirtualRuntime} based on the current tasks.
     * <p>
     * This is called before queueing tasks, to ensure that {@link #minimumVirtualRuntime} is up-to-date.
     *
     * @param current The machine which we updating runtimes from.
     */
    @GuardedBy("computerLock")
    private void updateRuntimes(@Nullable ExecutorImpl current) {
        var minRuntime = Long.MAX_VALUE;

        // If we've a task on the queue, use that as our base time.
        if (!computerQueue.isEmpty()) minRuntime = computerQueue.first().virtualRuntime;

        // Update all the currently executing tasks
        var now = System.nanoTime();
        var tasks = 1 + computerQueue.size();
        for (@Nullable var runner : workersReadOnly()) {
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
     * @param executor The executor to requeue
     */
    private void afterWork(ExecutorImpl executor) {
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

    @SuppressWarnings("GuardedBy")
    private int computerQueueSize() {
        // FIXME: We access this on other threads (in TimeoutState), so their reads won't be consistent. This isn't
        //  "critical" behaviour, so not clear if it matters too much.
        return computerQueue.size();
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
        // +1 to include the current task
        var count = 1 + computerQueueSize();
        return count < LATENCY_MAX_TASKS ? latency / count : minPeriod;
    }

    /**
     * Determine if the thread has computers queued up.
     *
     * @return If we have work queued up.
     */
    @VisibleForTesting
    boolean hasPendingWork() {
        return computerQueueSize() > 0;
    }

    /**
     * Check if we have more work queued than we have capacity for. Effectively a more fine-grained version of
     * {@link #hasPendingWork()}.
     *
     * @return If the computer threads are busy.
     */
    private boolean isBusy() {
        return computerQueueSize() > idleWorkers.get();
    }

    private void workerFinished(WorkerThread worker) {
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
                assert false : "workerFinished but inconsistent worker";
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
     * Observes all currently active {@link WorkerThread}s and terminates their tasks once they have exceeded the hard
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
            var workerThreadIds = new long[workersReadOnly().length];
            Arrays.fill(workerThreadIds, Thread.currentThread().getId());

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

                checkRunners(workerThreadIds);
            }
        }

        private void checkRunners(long[] workerThreadIds) {
            var workers = workersReadOnly();

            long[] allocations;
            if (ThreadAllocations.isSupported()) {
                // If allocation tracking is supported, update the current thread IDs and then fetch the total allocated
                // memory. When dealing with multiple workers, it's more efficient to getAllocatedBytes in bulk rather
                // than, hence doing it within the worker loop.
                // However, this does mean we need to maintain an array of worker thread IDs. We could have a shared
                // array and update it within .addWorker(_), but that's got all sorts of thread-safety issues. It ends
                // up being easier (and not too inefficient) to just recompute the array each time.
                for (var i = 0; i < workers.length; i++) {
                    var runner = workers[i];
                    if (runner != null) workerThreadIds[i] = runner.owner.getId();
                }
                allocations = ThreadAllocations.getAllocatedBytes(workerThreadIds);
            } else {
                allocations = null;
            }
            var allocationTime = System.nanoTime();

            for (var i = 0; i < workers.length; i++) {
                var runner = workers[i];
                if (runner == null) continue;

                // If the worker has no work, skip
                var executor = runner.currentExecutor.get();
                if (executor == null) continue;

                // Refresh the timeout state. Will set the pause/soft timeout flags as appropriate.
                executor.timeout.refresh();

                // And track the allocated memory.
                if (allocations != null) {
                    executor.updateAllocations(new ThreadAllocation(workerThreadIds[i], allocations[i], allocationTime));
                }

                // If we're still within normal execution times (TIMEOUT) or soft abort (ABORT_TIMEOUT),
                // then we can let the Lua machine do its work.
                var remainingTime = executor.timeout.getRemainingTime();
                // If remainingTime > 0, then we're executing normally,
                // If remainingTime > -ABORT_TIMEOUT, then we've soft aborted.
                // Otherwise, remainingTime <= -ABORT_TIMEOUT, and we've run over by -ABORT_TIMEOUT - remainingTime.
                var afterHardAbort = -remainingTime - TimeoutState.ABORT_TIMEOUT;
                if (afterHardAbort < 0) continue;

                // Set the hard abort flag.
                executor.timeout.hardAbort();
                executor.worker.abortWithTimeout();

                if (afterHardAbort >= TimeoutState.ABORT_TIMEOUT * 2) {
                    // If we've hard aborted and interrupted, and we're still not dead, then mark the worker
                    // as dead, finish off the task, and spawn a new runner.
                    runner.reportTimeout(executor, remainingTime);
                    runner.owner.interrupt();

                    workerFinished(runner);
                } else if (afterHardAbort >= TimeoutState.ABORT_TIMEOUT) {
                    // If we've hard aborted but we're still not dead, dump the stack trace and interrupt
                    // the task.
                    runner.reportTimeout(executor, remainingTime);
                    runner.owner.interrupt();
                }
            }
        }
    }

    /**
     * Pulls tasks from the {@link #computerQueue} queue and runs them.
     * <p>
     * This is responsible for running the {@link ComputerScheduler.Worker#work()}, {@link ExecutorImpl#beforeWork()}
     * and {@link ExecutorImpl#afterWork()} functions. Everything else is either handled by the executor,
     * timeout state or monitor.
     */
    private final class WorkerThread implements Runnable {
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
         * @see #workerFinished(WorkerThread)
         */
        final AtomicBoolean running = new AtomicBoolean(true);

        /**
         * The computer we're currently running.
         */
        final AtomicReference<ExecutorImpl> currentExecutor = new AtomicReference<>(null);

        /**
         * The last time we reported a stack trace, used to avoid spamming the logs.
         */
        AtomicLong lastReport = new AtomicLong(Long.MIN_VALUE);

        WorkerThread(int index) {
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
            while (running.get()) {
                // Wait for an active queue to execute
                ExecutorImpl executor;
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

                // Mark this computer as executing.
                if (!ExecutorImpl.STATE.compareAndSet(executor, ExecutorState.ON_QUEUE, ExecutorState.RUNNING)) {
                    assert false : "Running computer on the wrong thread";
                    LOG.error(
                        "Trying to run computer #{} on thread {}, but already running on another thread. This is a SERIOUS " +
                            "bug, please report with your debug.log.",
                        executor.worker.getComputerID(), owner.getName()
                    );
                }

                // If we're stopping, the only thing this executor should be doing is shutting down.
                if (state.get() >= STOPPING) executor.worker.unload();

                // Reset the timers
                executor.beforeWork();

                // And then set the current executor. It's important to do it afterwards, as otherwise we introduce
                // race conditions with the monitor.
                currentExecutor.set(executor);

                // Execute the task
                try {
                    executor.worker.work();
                } catch (Exception | LinkageError | VirtualMachineError e) {
                    LOG.error("Error running task on computer #" + executor.worker.getComputerID(), e);
                    // Tear down the computer immediately. There's no guarantee it's well-behaved from now on.
                    executor.worker.abortWithError();
                } finally {
                    var thisExecutor = currentExecutor.getAndSet(null);
                    if (thisExecutor != null) afterWork(executor);
                }
            }
        }

        private void reportTimeout(ExecutorImpl executor, long time) {
            if (!LOG.isErrorEnabled(Logging.COMPUTER_ERROR)) return;

            // Attempt to debounce stack trace reporting, limiting ourselves to one every second. There's no need to be
            // ultra-precise in our atomics, as long as one of them wins!
            var now = System.nanoTime();
            var then = lastReport.get();
            if (then != Long.MIN_VALUE && now - then - REPORT_DEBOUNCE <= 0) return;
            if (!lastReport.compareAndSet(then, now)) return;

            var owner = Objects.requireNonNull(this.owner);

            var builder = new StringBuilder()
                .append("Terminating computer #").append(executor.worker.getComputerID())
                .append(" due to timeout (ran over by ").append(time * -1e-9)
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

            executor.worker.writeState(builder);

            LOG.warn(builder.toString());
        }
    }

    /**
     * The current state of a {@link ExecutorState}.
     * <p>
     * Executors are either enqueued (have more work to do) or not and working or not. This enum encapsulates the four
     * combinations of these properties, with the following transitions:
     *
     * <pre>{@code
     *            submit()            afterWork()
     *      IDLE ---------> ON_QUEUE <----------- REPEAT
     *       ^                  |                   ^
     *       |                  | runImpl()         |
     *       |                  V                   |
     *       +---------------RUNNING----------------+
     *   afterWork()                  submit()
     * }</pre>
     */
    enum ExecutorState {
        /**
         * This executor is idle.
         */
        IDLE,

        /**
         * This executor is on the queue but idle.
         */
        ON_QUEUE,

        /**
         * This executor is running and will transition to idle after execution.
         */
        RUNNING,

        /**
         * This executor is running and should run again after this task finishes.
         */
        REPEAT;

        ExecutorState enqueue() {
            return switch (this) {
                case IDLE, ON_QUEUE -> ON_QUEUE;
                case RUNNING, REPEAT -> REPEAT;
            };
        }

        ExecutorState requeue() {
            return switch (this) {
                case IDLE, ON_QUEUE -> {
                    assert false : "Impossible state after executing";
                    LOG.error("Impossible state - calling requeue with {}.", this);
                    yield ExecutorState.ON_QUEUE;
                }
                case RUNNING -> ExecutorState.IDLE;
                case REPEAT -> ExecutorState.ON_QUEUE;
            };
        }
    }

    private final class ExecutorImpl implements Executor {
        public static final AtomicReferenceFieldUpdater<ExecutorImpl, ExecutorState> STATE = AtomicReferenceFieldUpdater.newUpdater(
            ExecutorImpl.class, ExecutorState.class, "$state"
        );
        public static final AtomicReferenceFieldUpdater<ExecutorImpl, ThreadAllocation> THREAD_ALLOCATION = AtomicReferenceFieldUpdater.newUpdater(
            ExecutorImpl.class, ThreadAllocation.class, "$threadAllocation"
        );

        final Worker worker;
        private final MetricsObserver metrics;
        final TimeoutImpl timeout;

        /**
         * The current state of this worker.
         */
        @Keep
        private volatile ExecutorState $state = ExecutorState.IDLE;

        /**
         * Information about allocations on the currently executing thread.
         * <p>
         * {@linkplain #beforeWork() Before starting any work}, we set this to the current thread and the current
         * {@linkplain ThreadAllocations#getAllocatedBytes(long) amount of allocated memory}. When the computer
         * {@linkplain #afterWork()} finishes executing, we set this back to null and compute the difference between the
         * two, updating the {@link Metrics#JAVA_ALLOCATION} metric.
         */
        @Keep
        private volatile @Nullable ThreadAllocation $threadAllocation = null;

        /**
         * The amount of time this computer has used on a theoretical machine which shares work evenly amongst computers.
         *
         * @see ComputerThread
         */
        long virtualRuntime = 0;

        /**
         * The last time at which we updated {@link #virtualRuntime}.
         *
         * @see ComputerThread
         */
        long vRuntimeStart;

        ExecutorImpl(Worker worker, MetricsObserver metrics) {
            this.worker = worker;
            this.metrics = metrics;
            timeout = new TimeoutImpl();
        }

        /**
         * Called before calling {@link Worker#work()}, setting up any important state.
         */
        void beforeWork() {
            vRuntimeStart = System.nanoTime();
            timeout.startTimer(scaledPeriod());

            if (ThreadAllocations.isSupported()) {
                var current = Thread.currentThread().getId();
                THREAD_ALLOCATION.set(this, new ThreadAllocation(current, ThreadAllocations.getAllocatedBytes(current), System.nanoTime()));
            }
        }

        /**
         * Called after executing {@link Worker#work()}.
         *
         * @return If we have more work to do.
         */
        boolean afterWork() {
            timeout.reset();
            metrics.observe(Metrics.COMPUTER_TASKS, timeout.getExecutionTime());

            if (ThreadAllocations.isSupported()) {
                var current = Thread.currentThread().getId();
                var info = THREAD_ALLOCATION.getAndSet(this, null);
                assert info.threadId() == current;

                var allocatedTotal = ThreadAllocations.getAllocatedBytes(current);
                var allocated = allocatedTotal - info.allocatedBytes();
                if (allocated > 0) {
                    metrics.observe(Metrics.JAVA_ALLOCATION, allocated);
                } else if (allocated < 0) {
                    LOG.warn(
                        """
                            Allocated a negative number of bytes ({})!
                                Previous measurement at t={} on Thread #{} = {}
                                 Current measurement at t={} on Thread #{} = {}""",
                        allocated,
                        info.time(), info.threadId(), info.allocatedBytes(),
                        System.nanoTime(), current, allocatedTotal
                    );
                }
            }

            var state = STATE.getAndUpdate(this, ExecutorState::requeue);
            return state == ExecutorState.REPEAT;
        }

        /**
         * Update the per-thread allocation information.
         *
         * @param allocation The latest allocation information.
         */
        void updateAllocations(ThreadAllocation allocation) {
            ThreadAllocation current;
            long allocated;
            do {
                // Probe the current information - if it's null or the thread has changed, then the worker has already
                // finished and this information is out-of-date, so just abort.
                current = THREAD_ALLOCATION.get(this);
                if (current == null || current.threadId() != allocation.threadId()) return;

                // Then compute the difference since the previous measurement. If the new value is less than the current
                // one, then it must be out-of-date. Again, just abort.
                allocated = allocation.allocatedBytes() - current.allocatedBytes();
                if (allocated <= 0) return;
            } while (!THREAD_ALLOCATION.compareAndSet(this, current, allocation));

            metrics.observe(Metrics.JAVA_ALLOCATION, allocated);
        }

        @Override
        public void submit() {
            var state = STATE.getAndUpdate(this, ExecutorState::enqueue);
            if (state == ExecutorState.IDLE) queue(this);
        }

        @Override
        public TimeoutState timeoutState() {
            return timeout;
        }

        @Override
        public long getRemainingTime() {
            return timeout.getRemainingTime();
        }

        @Override
        public void setRemainingTime(long time) {
            timeout.setRemainingTime(time);
        }
    }

    private final class TimeoutImpl extends ManagedTimeoutState {
        @Override
        protected boolean shouldPause() {
            return hasPendingWork();
        }
    }

    /**
     * Allocation information about a specific thread.
     *
     * @param threadId       The ID of this thread.
     * @param allocatedBytes The amount of memory this thread has allocated.
     * @param time           The time (in nanoseconds) when this time was computed.
     */
    private record ThreadAllocation(long threadId, long allocatedBytes, long time) {
    }
}
