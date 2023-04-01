// SPDX-FileCopyrightText: 2020 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.core.asm;

import dan200.computercraft.api.peripheral.PeripheralType;

import javax.annotation.Nullable;

public final class NamedMethod<T> {
    private final String name;
    private final T method;
    private final boolean nonYielding;

    private final @Nullable PeripheralType genericType;

    NamedMethod(String name, T method, boolean nonYielding, @Nullable PeripheralType genericType) {
        this.name = name;
        this.method = method;
        this.nonYielding = nonYielding;
        this.genericType = genericType;
    }

    public String getName() {
        return name;
    }

    public T getMethod() {
        return method;
    }

    public boolean nonYielding() {
        return nonYielding;
    }

    @Nullable
    public PeripheralType getGenericType() {
        return genericType;
    }
}
