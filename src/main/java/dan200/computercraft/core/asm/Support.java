// SPDX-FileCopyrightText: 2023 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.core.asm;

/**
 * Support methods used by the generated ASM.
 */
public final class Support {
    private Support() {
    }

    public static Object[] of() {
        return new Object[]{};
    }

    public static Object[] of(Object value) {
        return new Object[]{ value };
    }
}
