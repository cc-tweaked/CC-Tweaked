// SPDX-FileCopyrightText: 2023 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.core.metrics;

/**
 * A {@link MetricsObserver} implementation which discards all metrics it receives.
 */
final class DiscardingMetricsObserver implements MetricsObserver {
    static final MetricsObserver INSTANCE = new DiscardingMetricsObserver();

    private DiscardingMetricsObserver() {
    }

    @Override
    public void observe(Metric.Counter counter) {
    }

    @Override
    public void observe(Metric.Event event, long value) {
    }
}
