// SPDX-FileCopyrightText: 2020 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.core.asm;

import dan200.computercraft.api.lua.*;

import java.util.Collections;

public interface LuaMethod {
    Generator<LuaMethod> GENERATOR = new Generator<>(LuaMethod.class, Collections.singletonList(ILuaContext.class),
        m -> (target, context, args) -> context.executeMainThreadTask(() -> ResultHelpers.checkNormalResult(m.apply(target, context, args)))
    );

    IntCache<LuaMethod> DYNAMIC = new IntCache<>(
        method -> (instance, context, args) -> ((IDynamicLuaObject) instance).callMethod(context, method, args)
    );

    String[] EMPTY_METHODS = new String[0];

    MethodResult apply(Object target, ILuaContext context, IArguments args) throws LuaException;
}
