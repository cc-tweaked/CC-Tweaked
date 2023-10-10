// SPDX-FileCopyrightText: 2019 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

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
