// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.core.metrics;

/**
 * Times how long an operation takes, observing the duration as a {@link Metric.Event}.
 */
public final class OperationTimer implements AutoCloseable {
    private final MetricsObserver observer;
    private final Metric.Event event;
    private final long start;

    private OperationTimer(MetricsObserver observer, Metric.Event event, long start) {
        this.observer = observer;
        this.event = event;
        this.start = start;
    }

    /**
     * Start a timer for an operation.
     *
     * @param observer The metrics observer to submit the operation to.
     * @param event    The event to observe. The resulting value will be a duration in nanoseconds, and so its
     *                 {@linkplain Metric#unit() unit} should be {@code "ns"}.
     * @return The running operation timer.
     */
    public static OperationTimer start(MetricsObserver observer, Metric.Event event) {
        return new OperationTimer(observer, event, System.nanoTime());
    }

    @Override
    public void close() {
        observer.observe(event, System.nanoTime() - start);
    }
}
