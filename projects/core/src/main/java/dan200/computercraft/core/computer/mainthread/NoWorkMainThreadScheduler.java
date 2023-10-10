// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.core.computer.mainthread;

import dan200.computercraft.core.metrics.MetricsObserver;

import java.util.concurrent.TimeUnit;

/**
 * A {@link MainThreadScheduler} which fails when a computer tries to enqueue work.
 * <p>
 * This is useful for emulators, where we'll never make any main thread calls.
 */
public final class NoWorkMainThreadScheduler implements MainThreadScheduler {
    @Override
    public Executor createExecutor(MetricsObserver observer) {
        return new ExecutorImpl();
    }

    private static final class ExecutorImpl implements Executor {
        @Override
        public boolean enqueue(Runnable task) {
            throw new IllegalStateException("Cannot schedule tasks");
        }

        @Override
        public boolean canWork() {
            return false;
        }

        @Override
        public boolean shouldWork() {
            return false;
        }

        @Override
        public void trackWork(long time, TimeUnit unit) {
        }
    }
}
