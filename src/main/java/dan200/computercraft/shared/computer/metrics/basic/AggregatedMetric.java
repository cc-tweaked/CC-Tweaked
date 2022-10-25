/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.computer.metrics.basic;

import dan200.computercraft.core.metrics.Metric;
import dan200.computercraft.core.metrics.Metrics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;

import java.util.Arrays;
import java.util.stream.Stream;

/**
 * An aggregate of a specific metric.
 */
public class AggregatedMetric
{
    private static final String TRANSLATION_PREFIX = "tracking_field.computercraft.";

    private final Metric metric;
    private final Aggregate aggregate;

    public AggregatedMetric( Metric metric, Aggregate aggregate )
    {
        this.metric = metric;
        this.aggregate = aggregate;
    }

    public Metric metric()
    {
        return metric;
    }

    public Aggregate aggregate()
    {
        return aggregate;
    }

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
        TranslatableComponent name = new TranslatableComponent( TRANSLATION_PREFIX + metric().name() + ".name" );
        return aggregate() == Aggregate.NONE ? name : new TranslatableComponent( TRANSLATION_PREFIX + aggregate().id(), name );
    }
}
