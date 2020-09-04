/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.computer.core;

import javax.annotation.Nonnull;

import net.minecraft.util.StringIdentifiable;

public enum ComputerState implements StringIdentifiable {
    OFF("off"), ON("on"), BLINKING("blinking");

    private final String name;

    ComputerState(String name) {
        this.name = name;
    }

    @Nonnull
    @Override
    public String asString() {
        return this.name;
    }

    @Override
    public String toString() {
        return this.name;
    }
}
