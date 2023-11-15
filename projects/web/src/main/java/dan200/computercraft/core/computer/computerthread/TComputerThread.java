// SPDX-FileCopyrightText: 2023 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.core.computer.computerthread;

import cc.tweaked.web.js.Callbacks;
import dan200.computercraft.core.computer.TimeoutState;
import dan200.computercraft.core.metrics.MetricsObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.teavm.jso.browser.TimerHandler;

import java.util.ArrayDeque;
import java.util.concurrent.TimeUnit;

/**
 * An implementation of {@link ComputerScheduler} which executes work as soon as possible via
 * {@link Callbacks#setImmediate(TimerHandler)}.
 * <p>
 * Timeouts are instead handled via polling, see {@link cc.tweaked.web.builder.PatchCobalt}.
 *
 * @see ComputerThread
 */
public class TComputerThread implements ComputerScheduler {
    private static final Logger LOG = LoggerFactory.getLogger(TComputerThread.class);
    private static final long SCALED_PERIOD = 50 * 1_000_000L;

    private static final ArrayDeque<ExecutorImpl> executors = new ArrayDeque<>();
    private static final TimerHandler callback = TComputerThread::workOnce;
    private static boolean enqueued;

    public TComputerThread(int threads) {
    }

    @Override
    public Executor createExecutor(Worker worker, MetricsObserver metrics) {
        return new ExecutorImpl(worker);
    }

    private static void workOnce() {
        enqueued = false;

        var executor = executors.poll();
        if (executor == null) throw new IllegalStateException("Working, but executor is null");

        executor.beforeWork();
        try {
            executor.worker.work();
        } catch (Exception e) {
            LOG.error("Error running computer", e);
            executor.worker.abortWithError();
        }
        executor.afterWork();

        if (!executors.isEmpty()) enqueue();
    }

    private static void enqueue() {
        if (enqueued) return;

        enqueued = true;
        Callbacks.setImmediate(callback);
    }

    @Override
    public boolean stop(long timeout, TimeUnit unit) {
        return true;
    }

    /**
     * The {@link Executor} for our scheduler.
     */
    private static final class ExecutorImpl implements ComputerScheduler.Executor {
        final ComputerScheduler.Worker worker;
        private final TimeoutImpl timeout = new TimeoutImpl();
        private boolean onQueue;

        private ExecutorImpl(Worker worker) {
            this.worker = worker;
        }

        @Override
        public void submit() {
            if (onQueue) return;
            onQueue = true;

            executors.add(this);
            enqueue();
        }

        void beforeWork() {
            if (!onQueue) throw new IllegalArgumentException("Working but not on queue");
            onQueue = false;
            timeout.startTimer(SCALED_PERIOD);
        }

        void afterWork() {
            timeout.reset();
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

    private static final class TimeoutImpl extends ManagedTimeoutState {
        @Override
        protected boolean shouldPause() {
            return true;
        }
    }
}
