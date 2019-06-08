/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.util;

import net.minecraft.util.IIntArray;

@FunctionalInterface
public interface SingleIntArray extends IIntArray
{
    int get();

    @Override
    default int func_221476_a( int property )
    {
        return property == 0 ? get() : 0;
    }

    @Override
    default void func_221477_a( int i, int i1 )
    {

    }

    @Override
    default int func_221478_a()
    {
        return 1;
    }
}
