/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2018. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.peripheral.modem;

import net.minecraft.util.IStringSerializable;

import javax.annotation.Nonnull;

public enum BlockCableCableVariant implements IStringSerializable
{
    NONE( "none" ),
    ANY( "any" ),
    X_AXIS( "x" ),
    Y_AXIS( "y" ),
    Z_AXIS( "z" ),;

    private final String m_name;

    BlockCableCableVariant( String name )
    {
        m_name = name;
    }

    @Override
    @Nonnull
    public String getName()
    {
        return m_name;
    }
}
