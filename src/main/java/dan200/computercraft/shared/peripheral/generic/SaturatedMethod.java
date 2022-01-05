/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.peripheral.generic;

import dan200.computercraft.api.lua.IArguments;
import dan200.computercraft.api.lua.ILuaContext;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.MethodResult;
import dan200.computercraft.api.peripheral.IComputerAccess;
import dan200.computercraft.core.asm.NamedMethod;
import dan200.computercraft.core.asm.PeripheralMethod;

import javax.annotation.Nonnull;

final class SaturatedMethod
{
    private final Object target;
    private final String name;
    private final PeripheralMethod method;

    SaturatedMethod( Object target, NamedMethod<PeripheralMethod> method )
    {
        this.target = target;
        name = method.getName();
        this.method = method.getMethod();
    }

    @Nonnull
    MethodResult apply( @Nonnull ILuaContext context, @Nonnull IComputerAccess computer, @Nonnull IArguments args ) throws LuaException
    {
        return method.apply( target, context, computer, args );
    }

    @Nonnull
    String getName()
    {
        return name;
    }

    @Override
    public boolean equals( Object obj )
    {
        if( obj == this ) return true;
        if( !(obj instanceof SaturatedMethod other) ) return false;

        return method == other.method && target.equals( other.target );
    }

    @Override
    public int hashCode()
    {
        return 31 * target.hashCode() + method.hashCode();
    }
}
