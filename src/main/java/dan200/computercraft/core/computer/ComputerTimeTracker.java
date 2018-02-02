package dan200.computercraft.core.computer;

import com.google.common.collect.MapMaker;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.ref.WeakReference;
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
public class ComputerTimeTracker
{
    public static class Timings
    {
        private final WeakReference<Computer> computer;
        private final int computerId;

        private int tasks;

        private long totalTime;
        private long maxTime;

        public Timings( @Nonnull Computer computer )
        {
            this.computer = new WeakReference<>( computer );
            this.computerId = computer.getID();
        }

        @Nullable
        public Computer getComputer()
        {
            return computer.get();
        }

        public int getComputerId()
        {
            return computerId;
        }

        public int getTasks()
        {
            return tasks;
        }

        public long getTotalTime()
        {
            return totalTime;
        }

        public long getMaxTime()
        {
            return maxTime;
        }

        public double getAverage()
        {
            return totalTime / (double) tasks;
        }

        void update( long time )
        {
            tasks++;
            totalTime += time;
            if( time > maxTime ) maxTime = time;
        }
    }

    private static boolean tracking;
    private static final List<Timings> timings = new ArrayList<>();
    private static final Map<Computer, Timings> timingLookup = new MapMaker().weakKeys().makeMap();

    public synchronized static void start()
    {
        tracking = true;

        timings.clear();
        timingLookup.clear();
    }

    public synchronized static boolean stop()
    {
        if( !tracking ) return false;

        tracking = false;
        timingLookup.clear();
        return true;
    }

    public static synchronized List<Timings> getTimings()
    {
        return new ArrayList<>( timings );
    }

    public static synchronized void addTiming( Computer computer, long time )
    {
        if( !tracking ) return;

        Timings timings = ComputerTimeTracker.timingLookup.get( computer );
        if( timings == null )
        {
            timings = new Timings( computer );
            timingLookup.put( computer, timings );
            ComputerTimeTracker.timings.add( timings );
        }

        timings.update( time );
    }
}
