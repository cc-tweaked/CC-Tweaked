package dan200.computercraft.core.tracking;

import com.google.common.collect.MapMaker;
import dan200.computercraft.core.computer.Computer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Tracks timing information about computers, including how long they ran for
 * and the number of events they handled.
 *
 * Note that this <em>will</em> track computers which have been deleted (hence
 * the presence of {@link #timingLookup} and {@link #timings}
 */
public class TrackingContext
{
    private boolean tracking = false;

    private final List<ComputerTracker> timings = new ArrayList<>();
    private final Map<Computer, ComputerTracker> timingLookup = new MapMaker().weakKeys().makeMap();

    public synchronized void start()
    {
        if( !tracking ) Tracking.tracking.incrementAndGet();
        tracking = true;

        timings.clear();
        timingLookup.clear();
    }

    public synchronized boolean stop()
    {
        if( !tracking ) return false;

        Tracking.tracking.decrementAndGet();
        tracking = false;
        timingLookup.clear();
        return true;
    }

    public synchronized List<ComputerTracker> getTimings()
    {
        ArrayList<ComputerTracker> timings = new ArrayList<>( this.timings.size() );
        for( ComputerTracker timing : this.timings ) timings.add( new ComputerTracker( timing ) );
        return timings;
    }

    public void addTiming( Computer computer, long time )
    {
        if( !tracking ) return;

        synchronized( this )
        {
            ComputerTracker computerTimings = timingLookup.get( computer );
            if( computerTimings == null )
            {
                computerTimings = new ComputerTracker( computer );
                timingLookup.put( computer, computerTimings );
                timings.add( computerTimings );
            }

            computerTimings.addTiming( time );
        }
    }

    public synchronized void addValue( Computer computer, TrackingField field, long change )
    {
        if( !tracking ) return;

        synchronized( this )
        {
            ComputerTracker computerTimings = timingLookup.get( computer );
            if( computerTimings == null )
            {
                computerTimings = new ComputerTracker( computer );
                timingLookup.put( computer, computerTimings );
                timings.add( computerTimings );
            }

            computerTimings.addValue( field, change );
        }
    }
}
