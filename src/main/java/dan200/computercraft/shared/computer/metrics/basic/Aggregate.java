/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
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
