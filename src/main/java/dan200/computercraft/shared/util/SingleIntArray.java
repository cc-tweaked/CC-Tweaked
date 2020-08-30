/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.util;

import net.minecraft.screen.PropertyDelegate;

@FunctionalInterface
public interface SingleIntArray extends PropertyDelegate
{
    int get();

    @Override
    default int get( int property )
    {
        return property == 0 ? get() : 0;
    }

    @Override
    default void set( int property, int value )
    {
    }

    @Override
    default int size()
    {
        return 1;
    }
}
