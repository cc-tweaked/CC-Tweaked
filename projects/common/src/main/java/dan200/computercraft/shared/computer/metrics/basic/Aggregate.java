// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.computer.metrics.basic;

import dan200.computercraft.core.metrics.Metric;

/**
 * An aggregate over a {@link Metric}.
 * <p>
 * Only {@link Metric.Event events} support non-{@link Aggregate#NONE} aggregates.
 */
public enum Aggregate {
    NONE("none"),
    COUNT("count"),
    AVG("avg"),
    MAX("max");

    private final String id;

    Aggregate(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }
}
