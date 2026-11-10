// SPDX-FileCopyrightText: 2023 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.core.asm;

import dan200.computer.core.ILuaObject;
import dan200.computercraft.api.lua.MethodResult;
import dan200.computercraft.core.methods.LuaMethod;
import dan200.computercraft.core.methods.MethodSupplier;

import java.util.List;
import java.util.Objects;

/**
 * Provides a {@link MethodSupplier} for {@link LuaMethod}s.
 */
public final class LuaMethodSupplier {
    private static final Generator<LuaMethod> GENERATOR = new Generator<>(LuaMethod.class, List.of());
    private static final IntCache<LuaMethod> DYNAMIC = new IntCache<>(
        method -> (instance, args) -> MethodResult.of(((ILuaObject) instance).callMethod(method, args.getAll()))
    );

    private LuaMethodSupplier() {
    }

    public static final MethodSupplier<LuaMethod> INSTANCE = new MethodSupplierImpl<>(GENERATOR, DYNAMIC, x -> x instanceof ILuaObject dynamic
        ? Objects.requireNonNull(dynamic.getMethodNames(), "Dynamic methods cannot be null")
        : null
    );
}
