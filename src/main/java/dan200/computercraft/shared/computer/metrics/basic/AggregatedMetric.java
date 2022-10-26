/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.computer.metrics.basic;

import dan200.computercraft.core.metrics.Metric;
import dan200.computercraft.core.metrics.Metrics;
import net.minecraft.network.chat.Component;

import java.util.Arrays;
import java.util.stream.Stream;

/**
 * An aggregate of a specific metric.
 *
 * @param metric    The metric we're aggregating.
 * @param aggregate The aggregate to use.
 */
public record AggregatedMetric(Metric metric, Aggregate aggregate)
{
    private static final String TRANSLATION_PREFIX = "tracking_field.computercraft.";

    public static Stream<AggregatedMetric> aggregatedMetrics()
    {
        Metrics.init();
        return Metric.metrics().values().stream()
            .flatMap( m -> m instanceof Metric.Counter
                ? Stream.of( new AggregatedMetric( m, Aggregate.NONE ) )
                : Arrays.stream( Aggregate.values() ).map( a -> new AggregatedMetric( m, a ) )
            );
    }

    public String name()
    {
        return aggregate() == Aggregate.NONE ? metric.name() : metric().name() + "_" + aggregate().id();
    }

    public Component displayName()
    {
        Component name = Component.translatable( TRANSLATION_PREFIX + metric().name() + ".name" );
        return aggregate() == Aggregate.NONE ? name : Component.translatable( TRANSLATION_PREFIX + aggregate().id(), name );
    }
}
