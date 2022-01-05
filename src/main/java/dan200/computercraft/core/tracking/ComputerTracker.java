/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.core.tracking;

import dan200.computercraft.core.computer.Computer;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;

import javax.annotation.Nullable;
import java.lang.ref.WeakReference;

public class ComputerTracker
{
    private final WeakReference<Computer> computer;
    private final int computerId;

    private long tasks;
    private long totalTime;
    private long maxTime;

    private long serverCount;
    private long serverTime;

    private final Object2LongOpenHashMap<TrackingField> fields;

    public ComputerTracker( Computer computer )
    {
        this.computer = new WeakReference<>( computer );
        computerId = computer.getID();
        fields = new Object2LongOpenHashMap<>();
    }

    ComputerTracker( ComputerTracker timings )
    {
        computer = timings.computer;
        computerId = timings.computerId;

        tasks = timings.tasks;
        totalTime = timings.totalTime;
        maxTime = timings.maxTime;

        serverCount = timings.serverCount;
        serverTime = timings.serverTime;

        fields = new Object2LongOpenHashMap<>( timings.fields );
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

    void addTaskTiming( long time )
    {
        tasks++;
        totalTime += time;
        if( time > maxTime ) maxTime = time;
    }

    void addMainTiming( long time )
    {
        serverCount++;
        serverTime += time;
    }

    void addValue( TrackingField field, long change )
    {
        synchronized( fields )
        {
            fields.addTo( field, change );
        }
    }

    public long get( TrackingField field )
    {
        if( field == TrackingField.TASKS ) return tasks;
        if( field == TrackingField.MAX_TIME ) return maxTime;
        if( field == TrackingField.TOTAL_TIME ) return totalTime;
        if( field == TrackingField.AVERAGE_TIME ) return tasks == 0 ? 0 : totalTime / tasks;

        if( field == TrackingField.SERVER_COUNT ) return serverCount;
        if( field == TrackingField.SERVER_TIME ) return serverTime;

        synchronized( fields )
        {
            return fields.getLong( field );
        }
    }

    public String getFormatted( TrackingField field )
    {
        return field.format( get( field ) );
    }
}
