/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.core.util;

import javax.annotation.Nullable;

public final class StringUtil {
    private StringUtil() {
    }

    public static String normaliseLabel(String label) {
        if (label == null) return null;

        var length = Math.min(32, label.length());
        var builder = new StringBuilder(length);
        for (var i = 0; i < length; i++) {
            var c = label.charAt(i);
            if ((c >= ' ' && c <= '~') || (c >= 161 && c <= 172) || (c >= 174 && c <= 255)) {
                builder.append(c);
            } else {
                builder.append('?');
            }
        }

        return builder.toString();
    }

    public static String toString(@Nullable Object value) {
        return value == null ? "" : value.toString();
    }
}
