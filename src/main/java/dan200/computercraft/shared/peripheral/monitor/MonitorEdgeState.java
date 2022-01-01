/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.peripheral.monitor;

import net.minecraft.util.StringRepresentable;

import javax.annotation.Nonnull;

import static dan200.computercraft.shared.peripheral.monitor.MonitorEdgeState.Flags.*;

public enum MonitorEdgeState implements StringRepresentable
{
    NONE( "none", 0 ),

    L( "l", LEFT ),
    R( "r", RIGHT ),
    LR( "lr", LEFT | RIGHT ),
    U( "u", UP ),
    D( "d", DOWN ),

    UD( "ud", UP | DOWN ),
    RD( "rd", RIGHT | DOWN ),
    LD( "ld", LEFT | DOWN ),
    RU( "ru", RIGHT | UP ),
    LU( "lu", LEFT | UP ),

    LRD( "lrd", LEFT | RIGHT | DOWN ),
    RUD( "rud", RIGHT | UP | DOWN ),
    LUD( "lud", LEFT | UP | DOWN ),
    LRU( "lru", LEFT | RIGHT | UP ),
    LRUD( "lrud", LEFT | RIGHT | UP | DOWN );

    private final String name;
    private final int flags;

    MonitorEdgeState( String name, int flags )
    {
        this.name = name;
        this.flags = flags;
    }

    private static final MonitorEdgeState[] BY_FLAG = new MonitorEdgeState[16];

    static
    {
        for( MonitorEdgeState state : values() )
        {
            BY_FLAG[state.flags] = state;
        }
    }

    public static MonitorEdgeState fromConnections( boolean up, boolean down, boolean left, boolean right )
    {
        return BY_FLAG[(up ? UP : 0) | (down ? DOWN : 0) | (left ? LEFT : 0) | (right ? RIGHT : 0)];
    }

    @Override
    public String toString()
    {
        return getSerializedName();
    }

    @Nonnull
    @Override
    public String getSerializedName()
    {
        return name;
    }

    static final class Flags
    {
        static final int UP = 1 << 0;
        static final int DOWN = 1 << 1;
        static final int LEFT = 1 << 2;
        static final int RIGHT = 1 << 3;
    }
}
