/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.computer.metrics.basic;

import dan200.computercraft.core.metrics.Metric;
import dan200.computercraft.shared.computer.core.ServerComputer;

import javax.annotation.Nullable;
import java.lang.ref.WeakReference;
import java.util.Arrays;

/**
 * Metrics for an individual computer.
 */
public final class ComputerMetrics
{
    private static final int DEFAULT_LEN = 16;

    private final WeakReference<ServerComputer> computer;
    private final int computerId;
    private long[] counts;
    private long[] totals;
    private long[] max;

    ComputerMetrics( ServerComputer computer )
    {
        this.computer = new WeakReference<>( computer );
        computerId = computer.getID();
        counts = new long[DEFAULT_LEN];
        totals = new long[DEFAULT_LEN];
        max = new long[DEFAULT_LEN];
    }

    ComputerMetrics( ComputerMetrics other )
    {
        computer = other.computer;
        computerId = other.computerId;
        counts = Arrays.copyOf( other.counts, other.counts.length );
        totals = Arrays.copyOf( other.totals, other.totals.length );
        max = Arrays.copyOf( other.max, other.max.length );
    }

    @Nullable
    public ServerComputer computer()
    {
        return computer.get();
    }

    public int computerId()
    {
        return computerId;
    }

    private static long get( long[] values, Metric metric )
    {
        return metric.id() >= values.length ? 0 : values[metric.id()];
    }

    private long avg( long total, long count )
    {
        return count == 0 ? 0 : total / count;
    }

    public long get( Metric metric, Aggregate aggregate )
    {
        if( metric instanceof Metric.Counter ) return get( counts, metric );
        if( metric instanceof Metric.Event )
        {
            switch( aggregate )
            {
                case NONE:
                    return get( totals, metric );
                case COUNT:
                    return get( counts, metric );
                case AVG:
                    return avg( get( totals, metric ), get( counts, metric ) );
                case MAX:
                    return get( max, metric );
                default:
                    throw new IllegalArgumentException();
            }
        }

        throw new IllegalArgumentException( "Unknown metric " + metric.name() );
    }

    public String getFormatted( Metric field, Aggregate aggregate )
    {
        long value = get( field, aggregate );
        switch( aggregate )
        {
            case COUNT:
                return Metric.formatDefault( value );
            case AVG:
            case MAX:
            case NONE:
                return field.format( value );
            default:
                throw new IllegalArgumentException();
        }
    }

    private void ensureCapacity( Metric metric )
    {
        if( metric.id() < counts.length ) return;

        int newCapacity = Math.max( metric.id(), counts.length * 2 );
        counts = Arrays.copyOf( counts, newCapacity );
        totals = Arrays.copyOf( totals, newCapacity );
        max = Arrays.copyOf( max, newCapacity );
    }

    void observe( Metric.Counter counter )
    {
        ensureCapacity( counter );
        counts[counter.id()]++;
    }

    void observe( Metric.Event event, long value )
    {
        ensureCapacity( event );
        counts[event.id()]++;
        totals[event.id()] += value;
        if( value > max[event.id()] ) max[event.id()] = value;
    }
}
