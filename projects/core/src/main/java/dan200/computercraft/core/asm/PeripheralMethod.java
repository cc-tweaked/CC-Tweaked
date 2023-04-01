// SPDX-FileCopyrightText: 2020 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.core.asm;

import dan200.computercraft.api.lua.IArguments;
import dan200.computercraft.api.lua.ILuaContext;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.MethodResult;
import dan200.computercraft.api.peripheral.IComputerAccess;
import dan200.computercraft.api.peripheral.IDynamicPeripheral;

import java.util.Arrays;

public interface PeripheralMethod {
    Generator<PeripheralMethod> GENERATOR = new Generator<>(PeripheralMethod.class, Arrays.asList(ILuaContext.class, IComputerAccess.class),
        m -> (target, context, computer, args) -> context.executeMainThreadTask(() -> ResultHelpers.checkNormalResult(m.apply(target, context, computer, args.escapes())))
    );

    IntCache<PeripheralMethod> DYNAMIC = new IntCache<>(
        method -> (instance, context, computer, args) -> ((IDynamicPeripheral) instance).callMethod(computer, context, method, args)
    );

    MethodResult apply(Object target, ILuaContext context, IComputerAccess computer, IArguments args) throws LuaException;
}
