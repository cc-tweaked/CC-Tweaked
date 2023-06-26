// SPDX-FileCopyrightText: 2023 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.core.asm;

import dan200.computercraft.api.lua.ILuaContext;
import dan200.computercraft.api.peripheral.IComputerAccess;
import dan200.computercraft.api.peripheral.IDynamicPeripheral;
import dan200.computercraft.core.methods.MethodSupplier;
import dan200.computercraft.core.methods.PeripheralMethod;

import java.util.List;
import java.util.Objects;

public class PeripheralMethodSupplier {
    private static final Generator<PeripheralMethod> GENERATOR = new Generator<>(PeripheralMethod.class, List.of(ILuaContext.class, IComputerAccess.class),
        m -> (target, context, computer, args) -> context.executeMainThreadTask(() -> ResultHelpers.checkNormalResult(m.apply(target, context, computer, args.escapes())))
    );
    private static final IntCache<PeripheralMethod> DYNAMIC = new IntCache<>(
        method -> (instance, context, computer, args) -> ((IDynamicPeripheral) instance).callMethod(computer, context, method, args)
    );

    public static MethodSupplier<PeripheralMethod> create() {
        return new MethodSupplierImpl<>(GENERATOR, DYNAMIC, x -> x instanceof IDynamicPeripheral dynamic
            ? Objects.requireNonNull(dynamic.getMethodNames(), "Dynamic methods cannot be null")
            : null
        );
    }
}
