package dan200.computercraft.core.tracking;

import dan200.computercraft.core.computer.Computer;
import gnu.trove.map.hash.TObjectLongHashMap;

import javax.annotation.Nullable;
import java.lang.ref.WeakReference;

public class ComputerTracker
{
    private final WeakReference<Computer> computer;
    private final int computerId;

    private long tasks;
    private long totalTime;
    private long maxTime;

    private final TObjectLongHashMap<TrackingField> fields;

    public ComputerTracker( Computer computer )
    {
        this.computer = new WeakReference<>( computer );
        this.computerId = computer.getID();
        this.fields = new TObjectLongHashMap<>();
    }

    ComputerTracker( ComputerTracker timings )
    {
        this.computer = timings.computer;
        this.computerId = timings.computerId;

        this.tasks = timings.tasks;
        this.totalTime = timings.totalTime;
        this.maxTime = timings.maxTime;
        this.fields = new TObjectLongHashMap<>( timings.fields );
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

    public long getTasks()
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

    public long getAverage()
    {
        return totalTime / tasks;
    }

    void addTiming( long time )
    {
        tasks++;
        totalTime += time;
        if( time > maxTime ) maxTime = time;
    }

    void addValue( TrackingField field, long change )
    {
        fields.adjustOrPutValue( field, change, change );
    }

    public long get( TrackingField field )
    {
        if( field == TrackingField.TASKS ) return tasks;
        if( field == TrackingField.MAX_TIME ) return maxTime;
        if( field == TrackingField.TOTAL_TIME ) return totalTime;
        if( field == TrackingField.AVERAGE_TIME ) return totalTime / tasks;

        return fields.get( field );
    }

    public String getFormatted( TrackingField field )
    {
        return field.format( get( field ) );
    }
}
