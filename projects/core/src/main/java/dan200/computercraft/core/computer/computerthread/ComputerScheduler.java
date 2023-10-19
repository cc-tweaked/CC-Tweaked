// SPDX-FileCopyrightText: 2023 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.core.computer.computerthread;

import dan200.computercraft.core.computer.TimeoutState;
import dan200.computercraft.core.metrics.MetricsObserver;

import java.util.concurrent.TimeUnit;

/**
 * The {@link ComputerScheduler} is responsible for executing computers on the computer thread(s).
 * <p>
 * This handles both scheduling the computers for work across multiple threads, as well as {@linkplain TimeoutState timing out}
 * or pausing the computer if they execute for too long.
 * <p>
 * This API is composed of two interfaces, a {@link Worker} and {@link Executor}. The {@link ComputerScheduler}
 * implementation will supply an {@link Executor}, while consuming classes should implement {@link Worker}.
 * <p>
 * In practice, this interface is only implemented by {@link ComputerThread} (and consumed by {@link dan200.computercraft.core.computer.ComputerExecutor}),
 * however this interface is useful to enforce separation of the two.
 *
 * @see ManagedTimeoutState
 */
public interface ComputerScheduler {
    Executor createExecutor(Worker worker, MetricsObserver metrics);

    boolean stop(long timeout, TimeUnit unit) throws InterruptedException;

    /**
     * The {@link Executor} holds the state of a {@link Worker} within the scheduler.
     * <p>
     * This is used to schedule the worker for execution, as well as providing some additional control over the
     * {@link TimeoutState}.
     */
    interface Executor {
        /**
         * Submit the executor to the scheduler, marking it as ready {@linkplain Worker#work() to run some work}.
         * <p>
         * This function is idempotent - if the executor is already queued, nothing will happen.
         */
        void submit();

        /**
         * Get the executor's {@link TimeoutState}.
         *
         * @return The executor's timeout state.
         */
        TimeoutState timeoutState();

        /**
         * Get the amount of time this computer can run for before being interrupted.
         * <p>
         * This value starts off as {@link TimeoutState#BASE_TIMEOUT}, but may be reduced by
         * {@link #setRemainingTime(long)}.
         *
         * @return The time this computer can run for being interrupted.
         * @see #getRemainingTime()
         */
        long getRemainingTime();

        /**
         * Set the amount of this computer can execute for before being interrupted.
         * <p>
         * This value will typically be {@link TimeoutState#TIMEOUT}, but may be a previous value of
         * {@link #getRemainingTime()} if the computer is resuming after {@linkplain TimeoutState#isPaused() being
         * paused}.
         *
         * @param time The time this computer can execute for.
         * @see #getRemainingTime()
         */
        void setRemainingTime(long time);
    }

    /**
     * A {@link Worker} is responsible for actually running the computer's code.
     * <p>
     * his handles {@linkplain Worker#work() running the actual computer logic}, as well as providing some additional
     * control methods.
     * <p>
     * This should be implemented by the consuming class.
     */
    interface Worker {
        /**
         * Perform any work that the computer needs to do, for instance turning on, shutting down or actually running
         * code.
         * <p>
         * If the computer needs to run immediately again, it should call {@link Executor#submit()} within this method.
         *
         * @throws InterruptedException If the computer has run for too long and must be terminated.
         */
        void work() throws InterruptedException;

        /**
         * Get the ID of this computer, used in error messages.
         *
         * @return This computers ID.
         */
        int getComputerID();

        /**
         * Write any useful debugging information computer to the provided buffer. This is used in log messages when the
         * computer has run for too long.
         *
         * @param output The buffer to write to.
         */
        void writeState(StringBuilder output);

        /**
         * Abort this whole computer due to a timeout.
         */
        void abortWithTimeout();

        /**
         * Abort this whole computer due to some internal error.
         */
        void abortWithError();

        /**
         * "Unload" this computer, shutting it down and preventing it from running again.
         * <p>
         * This is called by the scheduler when {@linkplain ComputerScheduler#stop(long, TimeUnit) it is stopped.}
         */
        void unload();
    }
}
