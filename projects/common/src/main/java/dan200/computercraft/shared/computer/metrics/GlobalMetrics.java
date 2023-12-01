// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.computer.metrics;

import dan200.computercraft.core.metrics.Metric;
import dan200.computercraft.core.metrics.MetricsObserver;
import dan200.computercraft.shared.computer.core.ServerComputer;
import dan200.computercraft.shared.computer.core.ServerContext;
import dan200.computercraft.shared.computer.metrics.basic.BasicComputerMetricsObserver;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

/**
 * The global metrics system.
 *
 * @see ServerContext#metrics() To obtain an instance of this system.
 */
public final class GlobalMetrics {
    volatile boolean enabled = false;
    final Object lock = new Object();
    final List<ComputerMetricsObserver> trackers = new ArrayList<>();

    private final HashMap<UUID, BasicComputerMetricsObserver> instances = new HashMap<>();

    /**
     * Get a metrics observer for a specific player. This will not be active until
     * {@link BasicComputerMetricsObserver#start()} is called.
     *
     * @param uuid The player's UUID.
     * @return The metrics instance for this player.
     */
    public BasicComputerMetricsObserver getMetricsInstance(UUID uuid) {
        synchronized (lock) {
            var context = instances.get(uuid);
            if (context == null) instances.put(uuid, context = new BasicComputerMetricsObserver(this));
            return context;
        }
    }

    /**
     * Add a new global metrics observer. This will receive metrics data for all computers.
     *
     * @param tracker The observer to add.
     * @return Whether the observer was added. {@code false} if the observer was already registered.
     */
    public boolean addObserver(ComputerMetricsObserver tracker) {
        synchronized (lock) {
            if (trackers.contains(tracker)) return false;
            trackers.add(tracker);
            enabled = true;
            return true;
        }
    }

    /**
     * Remove a previously-registered global metrics observer.
     *
     * @param tracker The observer to add.
     * @return Whether the observer was removed. {@code false} if the observer was not registered.
     */
    public boolean removeObserver(ComputerMetricsObserver tracker) {
        synchronized (lock) {
            var changed = trackers.remove(tracker);
            enabled = !trackers.isEmpty();
            return changed;
        }
    }

    /**
     * Create an observer for a computer. This will delegate to all registered {@link ComputerMetricsObserver}s.
     *
     * @param computer The computer to create the observer for.
     * @return The instantiated observer.
     */
    public MetricsObserver createMetricObserver(ServerComputer computer) {
        return new DispatchObserver(computer);
    }

    private final class DispatchObserver implements MetricsObserver {
        private final ServerComputer computer;

        private DispatchObserver(ServerComputer computer) {
            this.computer = computer;
        }

        @Override
        public void observe(Metric.Counter counter) {
            if (!enabled) return;
            synchronized (lock) {
                // TODO: The lock here is nasty and aggressive. However, in my benchmarks I've found it has about
                //  equivalent performance to a CoW list and atomics. Would be good to drill into this, as locks do not
                //  scale well.
                for (var observer : trackers) observer.observe(computer, counter);
            }
        }

        @Override
        public void observe(Metric.Event event, long value) {
            if (!enabled) return;
            synchronized (lock) {
                for (var observer : trackers) observer.observe(computer, event, value);
            }
        }
    }
}
