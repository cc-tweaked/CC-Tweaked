/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.computer.blocks;

import net.minecraft.util.IStringSerializable;

import javax.annotation.Nonnull;

public enum ComputerState implements IStringSerializable
{
    Off( "off" ),
    On( "on" ),
    Blinking( "blinking" );

    private static final ComputerState[] VALUES = ComputerState.values();

    // TODO: Move to dan200.computercraft.shared.computer.core in the future. We can't do it now
    //  as Plethora depends on it.

    private final String name;

    ComputerState( String name )
    {
        this.name = name;
    }

    @Nonnull
    @Override
    public String getName()
    {
        return name;
    }

    @Override
    public String toString()
    {
        return name;
    }

    public static ComputerState valueOf( int ordinal )
    {
        return ordinal < 0 || ordinal >= VALUES.length ? ComputerState.Off : VALUES[ordinal];
    }
}

