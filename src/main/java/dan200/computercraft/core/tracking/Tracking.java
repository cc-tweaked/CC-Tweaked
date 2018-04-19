package dan200.computercraft.core.tracking;

import dan200.computercraft.core.computer.Computer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public class Tracking
{
    static final AtomicInteger tracking = new AtomicInteger( 0 );

    private static final Object lock = new Object();
    private static final HashMap<UUID, TrackingContext> contexts = new HashMap<>();
    private static final List<Tracker> trackers = new ArrayList<>();

    public static TrackingContext getContext( UUID uuid )
    {
        synchronized( lock )
        {
            TrackingContext context = contexts.get( uuid );
            if( context == null ) contexts.put( uuid, context = new TrackingContext() );
            return context;
        }
    }

    public static void add( Tracker tracker )
    {
        synchronized( lock )
        {
            trackers.add( tracker );
            tracking.incrementAndGet();
        }
    }

    public static void addTiming( Computer computer, long time )
    {
        if( tracking.get() == 0 ) return;

        synchronized( contexts )
        {
            for( TrackingContext context : contexts.values() ) context.addTiming( computer, time );
            for( Tracker tracker : trackers ) tracker.addTiming( computer, time );
        }
    }

    public static void addValue( Computer computer, TrackingField field, long change )
    {
        if( tracking.get() == 0 ) return;

        synchronized( lock )
        {
            for( TrackingContext context : contexts.values() ) context.addValue( computer, field, change );
            for( Tracker tracker : trackers ) tracker.addValue( computer, field, change );
        }
    }

    public static void reset()
    {
        synchronized( lock )
        {
            contexts.clear();
            trackers.clear();
            tracking.set( 0 );
        }
    }
}
