/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2018. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.computer.core;

import net.minecraft.util.IStringSerializable;

import javax.annotation.Nonnull;

public enum ComputerState implements IStringSerializable
{
    OFF( "off" ),
    ON( "on" ),
    BLINKING( "blinking" );

    private static final ComputerState[] VALUES = ComputerState.values();

    private String m_name;

    ComputerState( String name )
    {
        m_name = name;
    }

    @Nonnull
    @Override
    public String getName()
    {
        return m_name;
    }

    @Override
    public String toString()
    {
        return getName();
    }

    public static ComputerState valueOf( int ordinal )
    {
        return ordinal < 0 || ordinal >= VALUES.length ? ComputerState.OFF : VALUES[ordinal];
    }
}

