// SPDX-FileCopyrightText: 2020 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.core.asm;

public final class NamedMethod<T> {
    private final String name;
    private final T method;
    private final boolean nonYielding;

    NamedMethod(String name, T method, boolean nonYielding) {
        this.name = name;
        this.method = method;
        this.nonYielding = nonYielding;
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
}
