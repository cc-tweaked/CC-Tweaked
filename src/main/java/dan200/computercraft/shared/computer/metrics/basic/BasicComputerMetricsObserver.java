/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.computer.metrics.basic;

import com.google.common.collect.MapMaker;
import dan200.computercraft.core.metrics.Metric;
import dan200.computercraft.shared.computer.core.ServerComputer;
import dan200.computercraft.shared.computer.metrics.ComputerMetricsObserver;
import dan200.computercraft.shared.computer.metrics.GlobalMetrics;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Tracks timing information about computers, including how long they ran for and the number of events they handled.
 * <p>
 * Note that this will retain timings for computers which have been deleted.
 */
public class BasicComputerMetricsObserver implements ComputerMetricsObserver
{
    private final GlobalMetrics owner;
    private boolean tracking = false;

    private final List<ComputerMetrics> timings = new ArrayList<>();
    private final Map<ServerComputer, ComputerMetrics> timingLookup = new MapMaker().weakKeys().makeMap();

    public BasicComputerMetricsObserver( GlobalMetrics owner )
    {
        this.owner = owner;
    }

    public synchronized void start()
    {
        if( !tracking ) owner.addObserver( this );
        tracking = true;

        timings.clear();
        timingLookup.clear();
    }

    public synchronized boolean stop()
    {
        if( !tracking ) return false;

        owner.removeObserver( this );
        tracking = false;
        timingLookup.clear();
        return true;
    }

    public synchronized List<ComputerMetrics> getSnapshot()
    {
        ArrayList<ComputerMetrics> timings = new ArrayList<>( this.timings.size() );
        for( ComputerMetrics timing : this.timings ) timings.add( new ComputerMetrics( timing ) );
        return timings;
    }

    public synchronized List<ComputerMetrics> getTimings()
    {
        return new ArrayList<>( timings );
    }

    private ComputerMetrics getMetrics( ServerComputer computer )
    {
        ComputerMetrics existing = timingLookup.get( computer );
        if( existing != null ) return existing;

        ComputerMetrics metrics = new ComputerMetrics( computer );
        timingLookup.put( computer, metrics );
        timings.add( metrics );
        return metrics;
    }

    @Override
    public synchronized void observe( ServerComputer computer, Metric.Counter counter )
    {
        getMetrics( computer ).observe( counter );
    }

    @Override
    public synchronized void observe( ServerComputer computer, Metric.Event event, long value )
    {
        getMetrics( computer ).observe( event, value );
    }
}
