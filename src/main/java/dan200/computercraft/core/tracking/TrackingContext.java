/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.core.tracking;

import com.google.common.collect.MapMaker;
import dan200.computercraft.core.computer.Computer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Tracks timing information about computers, including how long they ran for and the number of events they handled.
 *
 * Note that this <em>will</em> track computers which have been deleted (hence the presence of {@link #timingLookup} and {@link #timings}
 */
public class TrackingContext implements Tracker
{
    private final List<ComputerTracker> timings = new ArrayList<>();
    private final Map<Computer, ComputerTracker> timingLookup = new MapMaker().weakKeys()
        .makeMap();
    private boolean tracking = false;

    public synchronized void start()
    {
        if( !this.tracking )
        {
            Tracking.tracking.incrementAndGet();
        }
        this.tracking = true;

        this.timings.clear();
        this.timingLookup.clear();
    }

    public synchronized boolean stop()
    {
        if( !this.tracking )
        {
            return false;
        }

        Tracking.tracking.decrementAndGet();
        this.tracking = false;
        this.timingLookup.clear();
        return true;
    }

    public synchronized List<ComputerTracker> getImmutableTimings()
    {
        ArrayList<ComputerTracker> timings = new ArrayList<>( this.timings.size() );
        for( ComputerTracker timing : this.timings )
        {
            timings.add( new ComputerTracker( timing ) );
        }
        return timings;
    }

    public synchronized List<ComputerTracker> getTimings()
    {
        return new ArrayList<>( this.timings );
    }

    @Override
    public void addTaskTiming( Computer computer, long time )
    {
        if( !this.tracking )
        {
            return;
        }

        synchronized( this )
        {
            ComputerTracker computerTimings = this.timingLookup.get( computer );
            if( computerTimings == null )
            {
                computerTimings = new ComputerTracker( computer );
                this.timingLookup.put( computer, computerTimings );
                this.timings.add( computerTimings );
            }

            computerTimings.addTaskTiming( time );
        }
    }

    @Override
    public void addServerTiming( Computer computer, long time )
    {
        if( !this.tracking )
        {
            return;
        }

        synchronized( this )
        {
            ComputerTracker computerTimings = this.timingLookup.get( computer );
            if( computerTimings == null )
            {
                computerTimings = new ComputerTracker( computer );
                this.timingLookup.put( computer, computerTimings );
                this.timings.add( computerTimings );
            }

            computerTimings.addMainTiming( time );
        }
    }

    @Override
    public void addValue( Computer computer, TrackingField field, long change )
    {
        if( !this.tracking )
        {
            return;
        }

        synchronized( this )
        {
            ComputerTracker computerTimings = this.timingLookup.get( computer );
            if( computerTimings == null )
            {
                computerTimings = new ComputerTracker( computer );
                this.timingLookup.put( computer, computerTimings );
                this.timings.add( computerTimings );
            }

            computerTimings.addValue( field, change );
        }
    }
}
