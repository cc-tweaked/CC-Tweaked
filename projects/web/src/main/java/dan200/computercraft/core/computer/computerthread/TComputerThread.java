// SPDX-FileCopyrightText: 2023 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.core.computer.computerthread;

import cc.tweaked.web.js.Callbacks;
import org.teavm.jso.browser.TimerHandler;

import java.util.ArrayDeque;
import java.util.concurrent.TimeUnit;

/**
 * A reimplementation of {@link ComputerThread} which, well, avoids any threading!
 * <p>
 * This instead just exucutes work as soon as possible via {@link Callbacks#setImmediate(TimerHandler)}. Timeouts are
 * instead handled via polling, see {@link cc.tweaked.web.builder.PatchCobalt}.
 */
public class TComputerThread {
    private static final ArrayDeque<ComputerExecutor> executors = new ArrayDeque<>();
    private final TimerHandler callback = this::workOnce;

    public TComputerThread(int threads) {
    }

    public void queue(ComputerExecutor executor) {
        if (executor.onComputerQueue) throw new IllegalStateException("Cannot queue already queued executor");
        executor.onComputerQueue = true;

        if (executors.isEmpty()) Callbacks.setImmediate(callback);
        executors.add(executor);
    }

    private void workOnce() {
        var executor = executors.poll();
        if (executor == null) throw new IllegalStateException("Working, but executor is null");
        if (!executor.onComputerQueue) throw new IllegalArgumentException("Working but not on queue");

        executor.beforeWork();
        try {
            executor.work();
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (executor.afterWork()) executors.push(executor);
        if (!executors.isEmpty()) Callbacks.setImmediate(callback);
    }

    public boolean hasPendingWork() {
        return true;
    }

    public long scaledPeriod() {
        return 50 * 1_000_000L;
    }

    public boolean stop(long timeout, TimeUnit unit) {
        return true;
    }
}
