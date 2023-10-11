// SPDX-FileCopyrightText: 2023 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.core.asm;

import dan200.computercraft.api.lua.IDynamicLuaObject;
import dan200.computercraft.api.lua.ILuaContext;
import dan200.computercraft.api.lua.MethodResult;
import dan200.computercraft.core.ComputerContext;
import dan200.computercraft.core.methods.LuaMethod;
import dan200.computercraft.core.methods.MethodSupplier;

import java.util.List;
import java.util.Objects;

/**
 * Provides a {@link MethodSupplier} for {@link LuaMethod}s.
 * <p>
 * This is used by {@link ComputerContext} to construct {@linkplain ComputerContext#peripheralMethods() the context-wide
 * method supplier}. It should not be used directly.
 */
public final class LuaMethodSupplier {
    private static final Generator<LuaMethod> GENERATOR = new Generator<>(List.of(ILuaContext.class),
        m -> (target, context, args) -> {
            try {
                return (MethodResult) m.invokeExact(target, context, args);
            } catch (Throwable t) {
                throw ResultHelpers.throwUnchecked(t);
            }
        },
        m -> (target, context, args) -> {
            var escArgs = args.escapes();
            return context.executeMainThreadTask(() -> ResultHelpers.checkNormalResult(m.apply(target, context, escArgs)));
        }
    );
    private static final IntCache<LuaMethod> DYNAMIC = new IntCache<>(
        method -> (instance, context, args) -> ((IDynamicLuaObject) instance).callMethod(context, method, args)
    );

    private LuaMethodSupplier() {
    }

    public static MethodSupplier<LuaMethod> create(List<GenericMethod> genericMethods) {
        return new MethodSupplierImpl<>(genericMethods, GENERATOR, DYNAMIC, x -> x instanceof IDynamicLuaObject dynamic
            ? Objects.requireNonNull(dynamic.getMethodNames(), "Dynamic methods cannot be null")
            : null
        );
    }
}
