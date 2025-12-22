// SPDX-FileCopyrightText: 2024 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.util;

import net.minecraft.util.FastColor;

/**
 * Utilities for working with 32-bit ARGB colours.
 *
 * @see FastColor.ARGB32
 */
public final class ARGB32 {
    private ARGB32() {
    }

    /**
     * Set the alpha channel to be fully transparent, removing the alpha channel component.
     *
     * @param colour The colour to make transparent.
     * @return The colour without the alpha channel.
     */
    public static int transparent(int colour) {
        return colour & 0xFFFFFF;
    }
}
