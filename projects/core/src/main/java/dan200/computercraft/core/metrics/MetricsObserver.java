// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.core.metrics;

import dan200.computercraft.core.computer.ComputerEnvironment;

/**
 * A metrics observer is used to report metrics for a single computer.
 * <p>
 * Various components (such as the computer scheduler or http API) will report statistics about their behaviour to the
 * observer. The observer may choose to consume these metrics, aggregating them and presenting them to the user in some
 * manner.
 *
 * @see ComputerEnvironment#getMetrics()
 * @see Metrics Built-in metrics which will be reported.
 */
public interface MetricsObserver {
    /**
     * Increment a counter by 1.
     *
     * @param counter The counter to observe.
     */
    void observe(Metric.Counter counter);

    /**
     * Observe a single instance of an event.
     *
     * @param event The event to observe.
     * @param value The value corresponding to this event.
     */
    void observe(Metric.Event event, long value);

    /**
     * Get a {@link MetricsObserver} which discards all metrics.
     *
     * @return An observer which discards all metrics.
     */
    static MetricsObserver discard() {
        return DiscardingMetricsObserver.INSTANCE;
    }
}
