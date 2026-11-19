// SPDX-FileCopyrightText: 2026 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.core.computer;

import dan200.computercraft.core.computer.computerthread.ComputerScheduler;
import dan200.computercraft.core.computer.computerthread.ManagedTimeoutState;
import dan200.computercraft.core.metrics.MetricsObserver;

import java.util.Queue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A {@link ComputerScheduler} that queues work, allowing it to be executed manually with {@link #work()}.
 */
public class MockComputerScheduler implements ComputerScheduler {
    private final Queue<ExecutorImpl> schedule = new LinkedBlockingDeque<>();

    @Override
    public Executor createExecutor(Worker worker, MetricsObserver metrics) {
        return new ExecutorImpl(this, worker);
    }

    @Override
    public boolean stop(long timeout, TimeUnit unit) {
        return true;
    }

    /**
     * Check whether this scheduler has work.
     *
     * @return Whether this scheduler has work.
     */
    public boolean hasWork() {
        return !schedule.isEmpty();
    }

    /**
     * Assert there is work to do, then perform it.
     */
    public void assertDoWork() throws InterruptedException {
        assertTrue(hasWork(), "Scheduler should have work to do");
        work();
    }

    /**
     * Do all currently scheduled work.
     */
    public void work() throws InterruptedException {
        while (true) {
            var executor = schedule.poll();
            if (executor == null) break;

            executor.scheduled = false;
            executor.worker.work();
        }
    }

    private static final class ExecutorImpl implements Executor {
        private final MockComputerScheduler scheduler;
        private boolean scheduled;

        private final Worker worker;
        private final ManagedTimeoutState timeout = new TimeoutImpl();

        private ExecutorImpl(MockComputerScheduler scheduler, Worker worker) {
            this.scheduler = scheduler;
            this.worker = worker;
        }

        @Override
        public void submit() {
            if (scheduled) return;
            scheduler.schedule.add(this);
            scheduled = true;
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
            return false;
        }
    }
}
