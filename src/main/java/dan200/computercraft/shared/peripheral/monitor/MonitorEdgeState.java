/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.peripheral.monitor;

import static dan200.computercraft.shared.peripheral.monitor.MonitorEdgeState.Flags.DOWN;
import static dan200.computercraft.shared.peripheral.monitor.MonitorEdgeState.Flags.LEFT;
import static dan200.computercraft.shared.peripheral.monitor.MonitorEdgeState.Flags.RIGHT;
import static dan200.computercraft.shared.peripheral.monitor.MonitorEdgeState.Flags.UP;

import javax.annotation.Nonnull;

import net.minecraft.util.StringIdentifiable;

public enum MonitorEdgeState implements StringIdentifiable {
    NONE("none", 0),

    L("l", LEFT),
    R("r", RIGHT),
    LR("lr", LEFT | RIGHT),
    U("u", UP),
    D("d", DOWN),

    UD("ud", UP | DOWN),
    RD("rd", RIGHT | DOWN),
    LD("ld", LEFT | DOWN),
    RU("ru", RIGHT | UP),
    LU("lu", LEFT | UP),

    LRD("lrd", LEFT | RIGHT | DOWN),
    RUD("rud", RIGHT | UP | DOWN),
    LUD("lud", LEFT | UP | DOWN),
    LRU("lru", LEFT | RIGHT | UP),
    LRUD("lrud", LEFT | RIGHT | UP | DOWN);

    private static final MonitorEdgeState[] BY_FLAG = new MonitorEdgeState[16];

    static {
        for (MonitorEdgeState state : values()) {
            BY_FLAG[state.flags] = state;
        }
    }

    private final String name;
    private final int flags;

    MonitorEdgeState(String name, int flags) {
        this.name = name;
        this.flags = flags;
    }

    public static MonitorEdgeState fromConnections(boolean up, boolean down, boolean left, boolean right) {
        return BY_FLAG[(up ? UP : 0) | (down ? DOWN : 0) | (left ? LEFT : 0) | (right ? RIGHT : 0)];
    }

    @Nonnull
    @Override
    public String asString() {
        return this.name;
    }

    static final class Flags {
        static final int UP = 1 << 0;
        static final int DOWN = 1 << 1;
        static final int LEFT = 1 << 2;
        static final int RIGHT = 1 << 3;
    }
}
