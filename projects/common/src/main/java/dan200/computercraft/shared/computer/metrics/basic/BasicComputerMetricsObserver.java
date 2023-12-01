// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.computer.metrics.basic;

import com.google.common.collect.MapMaker;
import dan200.computercraft.core.metrics.Metric;
import dan200.computercraft.shared.computer.core.ServerComputer;
import dan200.computercraft.shared.computer.metrics.ComputerMetricsObserver;
import dan200.computercraft.shared.computer.metrics.GlobalMetrics;

import javax.annotation.concurrent.GuardedBy;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Tracks timing information about computers, including how long they ran for and the number of events they handled.
 * <p>
 * Note that this will retain timings for computers which have been deleted.
 */
public class BasicComputerMetricsObserver implements ComputerMetricsObserver {
    private final GlobalMetrics owner;

    @GuardedBy("this")
    private final List<ComputerMetrics> timings = new ArrayList<>();

    @GuardedBy("this")
    private final Map<ServerComputer, ComputerMetrics> timingLookup = new MapMaker().weakKeys().makeMap();

    public BasicComputerMetricsObserver(GlobalMetrics owner) {
        this.owner = owner;
    }

    public void start() {
        if (!owner.addObserver(this)) return;

        synchronized (this) {
            timings.clear();
            timingLookup.clear();
        }
    }

    public boolean stop() {
        if (!owner.removeObserver(this)) return false;
        synchronized (this) {
            timingLookup.clear();
        }
        return true;
    }

    public synchronized List<ComputerMetrics> getSnapshot() {
        var timings = new ArrayList<ComputerMetrics>(this.timings.size());
        for (var timing : this.timings) timings.add(new ComputerMetrics(timing));
        return timings;
    }

    public synchronized List<ComputerMetrics> getTimings() {
        return new ArrayList<>(timings);
    }

    @GuardedBy("this")
    private ComputerMetrics getMetrics(ServerComputer computer) {
        var existing = timingLookup.get(computer);
        if (existing != null) return existing;

        var metrics = new ComputerMetrics(computer);
        timingLookup.put(computer, metrics);
        timings.add(metrics);
        return metrics;
    }

    @Override
    public synchronized void observe(ServerComputer computer, Metric.Counter counter) {
        getMetrics(computer).observe(counter);
    }

    @Override
    public synchronized void observe(ServerComputer computer, Metric.Event event, long value) {
        getMetrics(computer).observe(event, value);
    }
}
