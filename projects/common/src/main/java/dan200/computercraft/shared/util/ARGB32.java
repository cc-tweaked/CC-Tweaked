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
     * Set the alpha channel to be fully opaque.
     *
     * @param colour The colour to make opaque.
     * @return The fully-opaque colour
     */
    public static int opaque(int colour) {
        return 0xFF000000 | colour;
    }

    /**
     * Convert an ARGB32 colour to a {@linkplain FastColor.ABGR32 ABGR32} one.
     *
     * @param colour The colour to convert.
     * @return The converted colour.
     */
    public static int toABGR32(int colour) {
        // Swap B and R components, converting ARGB32 to ABGR32.
        return colour & 0xFF00FF00 | (colour & 0xFF0000) >> 16 | (colour & 0xFF) << 16;
    }
}
