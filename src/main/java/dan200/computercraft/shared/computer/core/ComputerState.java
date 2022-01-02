/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.computer.core;

import net.minecraft.util.StringRepresentable;

import javax.annotation.Nonnull;

public enum ComputerState implements StringRepresentable
{
    OFF( "off", "" ),
    ON( "on", "_on" ),
    BLINKING( "blinking", "_blink" );

    private final String name;
    private final String texture;

    ComputerState( String name, String texture )
    {
        this.name = name;
        this.texture = texture;
    }

    @Nonnull
    @Override
    public String getSerializedName()
    {
        return name;
    }

    @Override
    public String toString()
    {
        return name;
    }

    @Nonnull
    public String getTexture()
    {
        return texture;
    }
}
