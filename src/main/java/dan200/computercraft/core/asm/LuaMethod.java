/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.core.asm;

import dan200.computercraft.api.lua.*;

import javax.annotation.Nonnull;
import java.util.Collections;

public interface LuaMethod
{
    Generator<LuaMethod> GENERATOR = new Generator<>( LuaMethod.class, Collections.singletonList( ILuaContext.class ),
        m -> ( target, context, args ) -> context.executeMainThreadTask( () -> ResultHelpers.checkNormalResult( m.apply( target, context, args ) ) )
    );

    IntCache<LuaMethod> DYNAMIC = new IntCache<>(
        method -> ( instance, context, args ) -> ((IDynamicLuaObject) instance).callMethod( context, method, args )
    );

    String[] EMPTY_METHODS = new String[0];

    @Nonnull
    MethodResult apply( @Nonnull Object target, @Nonnull ILuaContext context, @Nonnull IArguments args ) throws LuaException;
}
