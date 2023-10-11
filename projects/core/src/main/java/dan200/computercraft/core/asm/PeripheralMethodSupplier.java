// SPDX-FileCopyrightText: 2023 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.core.asm;

import dan200.computercraft.api.lua.ILuaContext;
import dan200.computercraft.api.lua.MethodResult;
import dan200.computercraft.api.peripheral.IComputerAccess;
import dan200.computercraft.api.peripheral.IDynamicPeripheral;
import dan200.computercraft.core.ComputerContext;
import dan200.computercraft.core.methods.MethodSupplier;
import dan200.computercraft.core.methods.PeripheralMethod;

import java.util.List;
import java.util.Objects;

/**
 * Provides a {@link MethodSupplier} for {@link PeripheralMethod}s.
 * <p>
 * This is used by {@link ComputerContext} to construct {@linkplain ComputerContext#peripheralMethods() the context-wide
 * method supplier}. It should not be used directly.
 */
public final class PeripheralMethodSupplier {
    private static final Generator<PeripheralMethod> GENERATOR = new Generator<>(List.of(ILuaContext.class, IComputerAccess.class),
        m -> (target, context, computer, args) -> {
            try {
                return (MethodResult) m.invokeExact(target, context, computer, args);
            } catch (Throwable t) {
                throw ResultHelpers.throwUnchecked(t);
            }
        },
        m -> (target, context, computer, args) -> {
            var escArgs = args.escapes();
            return context.executeMainThreadTask(() -> ResultHelpers.checkNormalResult(m.apply(target, context, computer, escArgs)));
        }
    );
    private static final IntCache<PeripheralMethod> DYNAMIC = new IntCache<>(
        method -> (instance, context, computer, args) -> ((IDynamicPeripheral) instance).callMethod(computer, context, method, args)
    );

    private PeripheralMethodSupplier() {
    }

    public static MethodSupplier<PeripheralMethod> create(List<GenericMethod> genericMethods) {
        return new MethodSupplierImpl<>(genericMethods, GENERATOR, DYNAMIC, x -> x instanceof IDynamicPeripheral dynamic
            ? Objects.requireNonNull(dynamic.getMethodNames(), "Dynamic methods cannot be null")
            : null
        );
    }
}
