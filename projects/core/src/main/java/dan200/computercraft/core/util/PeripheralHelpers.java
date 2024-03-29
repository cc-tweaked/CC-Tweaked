// SPDX-FileCopyrightText: 2023 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.core.util;

import dan200.computercraft.api.peripheral.IPeripheral;

import javax.annotation.Nullable;

/**
 * Utilities for working with {@linkplain IPeripheral peripherals}.
 */
public final class PeripheralHelpers {
    private PeripheralHelpers() {
    }

    /**
     * Determine if two peripherals are equal. This is equivalent to {@link java.util.Objects#equals(Object, Object)},
     * but using {@link IPeripheral#equals(IPeripheral)} instead.
     *
     * @param a The first peripheral.
     * @param b The second peripheral.
     * @return If the two peripherals are equal.
     */
    public static boolean equals(@Nullable IPeripheral a, @Nullable IPeripheral b) {
        return a == b || (a != null && b != null && a.equals(b));
    }
}
