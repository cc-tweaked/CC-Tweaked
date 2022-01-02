/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.peripheral.monitor;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

final class MonitorState
{
    public static final MonitorState UNLOADED = new MonitorState( State.UNLOADED, null );
    public static final MonitorState MISSING = new MonitorState( State.MISSING, null );

    private final State state;
    private final TileMonitor monitor;

    private MonitorState( @Nonnull State state, @Nullable TileMonitor monitor )
    {
        this.state = state;
        this.monitor = monitor;
    }

    public static MonitorState present( @Nonnull TileMonitor monitor )
    {
        return new MonitorState( State.PRESENT, monitor );
    }

    public boolean isPresent()
    {
        return state == State.PRESENT;
    }

    public boolean isMissing()
    {
        return state == State.MISSING;
    }

    @Nullable
    public TileMonitor getMonitor()
    {
        return monitor;
    }

    enum State
    {
        UNLOADED,
        MISSING,
        PRESENT,
    }
}
