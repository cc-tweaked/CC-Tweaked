/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.command.arguments;

import dan200.computercraft.shared.command.Exceptions;
import dan200.computercraft.shared.computer.metrics.basic.AggregatedMetric;

public final class TrackingFieldArgumentType extends ChoiceArgumentType<AggregatedMetric> {
    private static final TrackingFieldArgumentType INSTANCE = new TrackingFieldArgumentType();

    private TrackingFieldArgumentType() {
        super(
            AggregatedMetric.aggregatedMetrics().toList(),
            AggregatedMetric::name, AggregatedMetric::displayName, Exceptions.TRACKING_FIELD_ARG_NONE
        );
    }

    public static TrackingFieldArgumentType metric() {
        return INSTANCE;
    }
}
