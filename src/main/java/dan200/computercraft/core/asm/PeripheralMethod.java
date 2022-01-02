/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.core.asm;

import dan200.computercraft.api.lua.IArguments;
import dan200.computercraft.api.lua.ILuaContext;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.MethodResult;
import dan200.computercraft.api.peripheral.IComputerAccess;
import dan200.computercraft.api.peripheral.IDynamicPeripheral;

import javax.annotation.Nonnull;
import java.util.Arrays;

public interface PeripheralMethod
{
    Generator<PeripheralMethod> GENERATOR = new Generator<>( PeripheralMethod.class, Arrays.asList( ILuaContext.class, IComputerAccess.class ),
        m -> ( target, context, computer, args ) -> context.executeMainThreadTask( () -> ResultHelpers.checkNormalResult( m.apply( target, context, computer, args ) ) )
    );

    IntCache<PeripheralMethod> DYNAMIC = new IntCache<>(
        method -> ( instance, context, computer, args ) -> ((IDynamicPeripheral) instance).callMethod( computer, context, method, args )
    );

    @Nonnull
    MethodResult apply( @Nonnull Object target, @Nonnull ILuaContext context, @Nonnull IComputerAccess computer, @Nonnull IArguments args ) throws LuaException;
}
