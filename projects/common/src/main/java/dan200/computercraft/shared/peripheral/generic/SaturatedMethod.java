// SPDX-FileCopyrightText: 2020 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.peripheral.generic;

import dan200.computercraft.api.lua.IArguments;
import dan200.computercraft.api.lua.ILuaContext;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.MethodResult;
import dan200.computercraft.api.peripheral.IComputerAccess;
import dan200.computercraft.core.methods.PeripheralMethod;

/**
 * A {@link PeripheralMethod} along with the method's target.
 */
final class SaturatedMethod {
    private final Object target;
    private final String name;
    private final PeripheralMethod method;

    SaturatedMethod(Object target, String name, PeripheralMethod method) {
        this.target = target;
        this.name = name;
        this.method = method;
    }

    MethodResult apply(ILuaContext context, IComputerAccess computer, IArguments args) throws LuaException {
        return method.apply(target, context, computer, args);
    }

    String getName() {
        return name;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (!(obj instanceof SaturatedMethod other)) return false;

        return method == other.method && target.equals(other.target);
    }

    @Override
    public int hashCode() {
        return 31 * target.hashCode() + method.hashCode();
    }
}
