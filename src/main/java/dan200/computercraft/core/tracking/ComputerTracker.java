/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.core.tracking;

import java.lang.ref.WeakReference;

import javax.annotation.Nullable;

import dan200.computercraft.core.computer.Computer;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;

public class ComputerTracker {
    private final WeakReference<Computer> computer;
    private final int computerId;
    private final Object2LongOpenHashMap<TrackingField> fields;
    private long tasks;
    private long totalTime;
    private long maxTime;
    private long serverCount;
    private long serverTime;

    public ComputerTracker(Computer computer) {
        this.computer = new WeakReference<>(computer);
        this.computerId = computer.getID();
        this.fields = new Object2LongOpenHashMap<>();
    }

    ComputerTracker(ComputerTracker timings) {
        this.computer = timings.computer;
        this.computerId = timings.computerId;

        this.tasks = timings.tasks;
        this.totalTime = timings.totalTime;
        this.maxTime = timings.maxTime;

        this.serverCount = timings.serverCount;
        this.serverTime = timings.serverTime;

        this.fields = new Object2LongOpenHashMap<>(timings.fields);
    }

    @Nullable
    public Computer getComputer() {
        return this.computer.get();
    }

    public int getComputerId() {
        return this.computerId;
    }

    public long getTasks() {
        return this.tasks;
    }

    public long getTotalTime() {
        return this.totalTime;
    }

    public long getMaxTime() {
        return this.maxTime;
    }

    public long getAverage() {
        return this.totalTime / this.tasks;
    }

    void addTaskTiming(long time) {
        this.tasks++;
        this.totalTime += time;
        if (time > this.maxTime) {
            this.maxTime = time;
        }
    }

    void addMainTiming(long time) {
        this.serverCount++;
        this.serverTime += time;
    }

    void addValue(TrackingField field, long change) {
        synchronized (this.fields) {
            this.fields.addTo(field, change);
        }
    }

    public String getFormatted(TrackingField field) {
        return field.format(this.get(field));
    }

    public long get(TrackingField field) {
        if (field == TrackingField.TASKS) {
            return this.tasks;
        }
        if (field == TrackingField.MAX_TIME) {
            return this.maxTime;
        }
        if (field == TrackingField.TOTAL_TIME) {
            return this.totalTime;
        }
        if (field == TrackingField.AVERAGE_TIME) {
            return this.tasks == 0 ? 0 : this.totalTime / this.tasks;
        }

        if (field == TrackingField.SERVER_COUNT) {
            return this.serverCount;
        }
        if (field == TrackingField.SERVER_TIME) {
            return this.serverTime;
        }

        synchronized (this.fields) {
            return this.fields.getLong(field);
        }
    }
}
