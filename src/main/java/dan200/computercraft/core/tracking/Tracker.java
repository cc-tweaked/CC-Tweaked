package dan200.computercraft.core.tracking;

import dan200.computercraft.core.computer.Computer;

public interface Tracker
{
    void addTiming( Computer computer, long time );

    void addValue( Computer computer, TrackingField field, long change );
}
