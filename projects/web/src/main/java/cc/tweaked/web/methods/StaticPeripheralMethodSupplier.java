// SPDX-FileCopyrightText: 2023 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package cc.tweaked.web.methods;

import dan200.computercraft.api.peripheral.IComputerAccess;
import dan200.computercraft.core.methods.LuaMethod;
import dan200.computercraft.core.methods.MethodSupplier;
import dan200.computercraft.core.methods.PeripheralMethod;

/**
 * A {@link MethodSupplier} for {@link PeripheralMethod}s that lifts {@link LuaMethod}s to {@link PeripheralMethod}.
 * As none of our peripherals need {@link IComputerAccess}, this is entirely safe.
 */
public final class StaticPeripheralMethodSupplier implements MethodSupplier<PeripheralMethod> {
    public static final StaticPeripheralMethodSupplier INSTANCE = new StaticPeripheralMethodSupplier();

    private StaticPeripheralMethodSupplier() {
    }

    @Override
    public boolean forEachSelfMethod(Object object, UntargetedConsumer<PeripheralMethod> consumer) {
        return StaticLuaMethodSupplier.INSTANCE.forEachSelfMethod(object, (name, method, info) -> consumer.accept(name, cast(method), null));
    }

    @Override
    public boolean forEachMethod(Object object, TargetedConsumer<PeripheralMethod> consumer) {
        return StaticLuaMethodSupplier.INSTANCE.forEachMethod(object, (target, name, method, info) -> consumer.accept(target, name, cast(method), null));
    }

    private static PeripheralMethod cast(LuaMethod method) {
        return (target, context, computer, args) -> method.apply(target, context, args);
    }
}
