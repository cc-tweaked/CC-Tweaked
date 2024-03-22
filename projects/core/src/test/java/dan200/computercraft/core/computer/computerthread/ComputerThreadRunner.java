// SPDX-FileCopyrightText: 2023 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.core.computer.computerthread;

import dan200.computercraft.core.computer.TimeoutState;
import dan200.computercraft.core.metrics.Metric;
import dan200.computercraft.core.metrics.MetricsObserver;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;

import javax.annotation.concurrent.GuardedBy;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiConsumer;

public class ComputerThreadRunner implements AutoCloseable {
    private final ComputerThread thread;

    private final Lock errorLock = new ReentrantLock();
    private final @GuardedBy("errorLock") Condition hasError = errorLock.newCondition();
    @GuardedBy("errorLock")
    private @MonotonicNonNull Throwable error = null;

    public ComputerThreadRunner() {
        this.thread = new ComputerThread(1);
    }

    public ComputerThread thread() {
        return thread;
    }

    @Override
    public void close() {
        try {
            if (!thread.stop(10, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Failed to shutdown ComputerContext in time.");
            }
        } catch (InterruptedException e) {
            throw new IllegalStateException("Runtime thread was interrupted", e);
        }
    }

    public Worker createWorker(BiConsumer<ComputerScheduler.Executor, TimeoutState> action) {
        return new Worker(thread, e -> action.accept(e, e.timeoutState()));
    }

    public void createLoopingComputer() {
        new Worker(thread, e -> {
            Thread.sleep(100);
            e.submit();
        }).executor().submit();
    }

    public void startAndWait(Worker worker) throws Exception {
        worker.executor().submit();
        do {
            errorLock.lock();
            try {
                rethrowIfNeeded();
                if (hasError.await(100, TimeUnit.MILLISECONDS)) rethrowIfNeeded();
            } finally {
                errorLock.unlock();
            }
        } while (!worker.executed || thread.hasPendingWork());
    }

    @GuardedBy("errorLock")
    private void rethrowIfNeeded() throws Exception {
        if (error != null) ComputerThreadRunner.<Exception>throwUnchecked0(error);
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> void throwUnchecked0(Throwable t) throws T {
        throw (T) t;
    }

    @FunctionalInterface
    private interface Task {
        void run(ComputerScheduler.Executor executor) throws InterruptedException;
    }

    public final class Worker implements ComputerScheduler.Worker, MetricsObserver {
        private final Task run;
        private final ComputerScheduler.Executor executor;
        private long[] totals = new long[16];
        private volatile boolean executed = false;

        private Worker(ComputerScheduler scheduler, Task run) {
            this.run = run;
            this.executor = scheduler.createExecutor(this, this);
        }

        public ComputerScheduler.Executor executor() {
            return executor;
        }

        @Override
        public void work() {
            try {
                run.run(executor);
                executed = true;
            } catch (Throwable e) {
                errorLock.lock();
                try {
                    if (error == null) {
                        error = e;
                        hasError.signal();
                    } else {
                        error.addSuppressed(e);
                    }
                } finally {
                    errorLock.unlock();
                }

                if (e instanceof Exception || e instanceof AssertionError) return;
                throwUnchecked0(e);
            }
        }

        @Override
        public int getComputerID() {
            return 0;
        }

        @Override
        public void writeState(StringBuilder output) {
        }

        @Override
        public void abortWithTimeout() {
        }

        @Override
        public void unload() {
        }

        @Override
        public void abortWithError() {
        }

        private synchronized void observeImpl(Metric metric, long value) {
            if (metric.id() >= totals.length) totals = Arrays.copyOf(totals, Math.max(metric.id(), totals.length * 2));
            totals[metric.id()] += value;
        }

        @Override
        public void observe(Metric.Counter counter) {
            observeImpl(counter, 1);
        }

        @Override
        public void observe(Metric.Event event, long value) {
            observeImpl(event, value);
        }

        public long getMetric(Metric metric) {
            var totals = this.totals;
            return metric.id() < totals.length ? totals[metric.id()] : 0;
        }
    }
}
