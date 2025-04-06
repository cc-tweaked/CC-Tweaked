// SPDX-FileCopyrightText: 2023 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.core.util;

import dan200.computercraft.api.lua.LuaException;

/**
 * A few helpers for working with arguments.
 * <p>
 * This should really be moved into the public API. However, until I have settled on a suitable format, we'll keep it
 * where it is used.
 */
public class ArgumentHelpers {
    public static void assertBetween(double value, double min, double max, String message) throws LuaException {
        if (value < min || value > max || Double.isNaN(value)) {
            throw new LuaException(String.format(message, "between " + min + " and " + max));
        }
    }

    public static void assertBetween(int value, int min, int max, String message) throws LuaException {
        if (value < min || value > max) {
            throw new LuaException(String.format(message, "between " + min + " and " + max));
        }
    }
}
