// SPDX-FileCopyrightText: 2023 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.core.asm;

import dan200.computercraft.api.lua.IDynamicLuaObject;
import dan200.computercraft.api.lua.ILuaContext;
import dan200.computercraft.core.methods.LuaMethod;
import dan200.computercraft.core.methods.MethodSupplier;
import org.jetbrains.annotations.VisibleForTesting;

import java.util.List;
import java.util.Objects;

public final class LuaMethodSupplier {
    @VisibleForTesting
    static final Generator<LuaMethod> GENERATOR = new Generator<>(LuaMethod.class, List.of(ILuaContext.class),
        m -> (target, context, args) -> context.executeMainThreadTask(() -> ResultHelpers.checkNormalResult(m.apply(target, context, args.escapes())))
    );
    private static final IntCache<LuaMethod> DYNAMIC = new IntCache<>(
        method -> (instance, context, args) -> ((IDynamicLuaObject) instance).callMethod(context, method, args)
    );

    private LuaMethodSupplier() {
    }

    public static MethodSupplier<LuaMethod> create() {
        return new MethodSupplierImpl<>(GENERATOR, DYNAMIC, x -> x instanceof IDynamicLuaObject dynamic
            ? Objects.requireNonNull(dynamic.getMethodNames(), "Dynamic methods cannot be null")
            : null
        );
    }
}
