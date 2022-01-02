/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.core.asm;

import dan200.computercraft.api.peripheral.PeripheralType;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class NamedMethod<T>
{
    private final String name;
    private final T method;
    private final boolean nonYielding;

    private final PeripheralType genericType;

    NamedMethod( String name, T method, boolean nonYielding, PeripheralType genericType )
    {
        this.name = name;
        this.method = method;
        this.nonYielding = nonYielding;
        this.genericType = genericType;
    }

    @Nonnull
    public String getName()
    {
        return name;
    }

    @Nonnull
    public T getMethod()
    {
        return method;
    }

    public boolean nonYielding()
    {
        return nonYielding;
    }

    @Nullable
    public PeripheralType getGenericType()
    {
        return genericType;
    }
}
