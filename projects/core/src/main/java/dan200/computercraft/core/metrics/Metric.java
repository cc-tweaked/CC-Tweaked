// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.core.metrics;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.LongFunction;

/**
 * A metric is some event which is emitted by a computer and observed by a {@link MetricsObserver}.
 * <p>
 * It comes in two forms: a simple {@link Metric.Counter} counts how many times an event has occurred (for instance,
 * how many HTTP requests have there been) while a {@link Metric.Event} has a discrete value for each event (for
 * instance, the number of bytes downloaded in this HTTP request). The values tied to a {@link Metric.Event}s can be
 * accumulated, and thus used to derive averages, rates and be sorted into histogram buckets.
 */
public abstract class Metric {
    private static final Map<String, Metric> allMetrics = new ConcurrentHashMap<>();
    private static final AtomicInteger nextId = new AtomicInteger();

    private final int id;
    private final String name;
    private final String unit;
    private final LongFunction<String> format;

    private Metric(String name, String unit, LongFunction<String> format) {
        if (allMetrics.containsKey(name)) throw new IllegalStateException("Duplicate key " + name);

        id = nextId.getAndIncrement();
        this.name = name;
        this.unit = unit;
        this.format = format;
        allMetrics.put(name, this);
    }

    /**
     * The unique ID for this metric.
     * <p>
     * Each metric is assigned a consecutive metric ID starting from 0. This allows you to store metrics in an array,
     * rather than a map.
     *
     * @return The metric's ID.
     */
    public int id() {
        return id;
    }

    /**
     * The unique name for this metric.
     *
     * @return The metric's name.
     */
    public String name() {
        return name;
    }

    /**
     * The unit used for this metric. This should be a lowercase "identifier-like" such as {@literal ms}. If no unit is
     * relevant, it can be empty.
     *
     * @return The metric's unit.
     */
    public String unit() {
        return unit;
    }

    /**
     * Format a value according to the metric's formatting rules. Implementations may choose to append units to the
     * returned value where relevant.
     *
     * @param value The value to format.
     * @return The formatted value.
     */
    public String format(long value) {
        return format.apply(value);
    }

    @Override
    public String toString() {
        return getClass().getName() + ":" + name();
    }

    /**
     * Get a map of all metrics.
     *
     * @return A map of all metrics.
     */
    public static Map<String, Metric> metrics() {
        return Collections.unmodifiableMap(allMetrics);
    }

    public static final class Counter extends Metric {
        public Counter(String id) {
            super(id, "", Metric::formatDefault);
        }
    }

    public static final class Event extends Metric {
        public Event(String id, String unit, LongFunction<String> format) {
            super(id, unit, format);
        }
    }

    public static String formatTime(long value) {
        return String.format("%.1fms", value * 1e-6);
    }

    public static String formatDefault(long value) {
        return String.format("%d", value);
    }

    private static final int KILOBYTE_SIZE = 1024;
    private static final String SI_PREFIXES = "KMGT";

    public static String formatBytes(long bytes) {
        if (bytes < KILOBYTE_SIZE) return String.format("%d B", bytes);
        var exp = (int) (Math.log((double) bytes) / Math.log(KILOBYTE_SIZE));
        if (exp > SI_PREFIXES.length()) exp = SI_PREFIXES.length();
        return String.format("%.1f %siB", bytes / Math.pow(KILOBYTE_SIZE, exp), SI_PREFIXES.charAt(exp - 1));
    }
}
