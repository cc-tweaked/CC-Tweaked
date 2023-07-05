// SPDX-FileCopyrightText: 2017 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.core.util;

import java.nio.charset.StandardCharsets;

public final class StringUtil {
    private StringUtil() {
    }

    public static String normaliseLabel(String label) {
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

    public static String byteStringToUtf8(String s){
        return String.valueOf(StandardCharsets.UTF_8.decode(StandardCharsets.ISO_8859_1.encode(s)));
    }

    public static String utfToByteString(String s){
        return String.valueOf(StandardCharsets.ISO_8859_1.decode(StandardCharsets.UTF_8.encode(s)));
    }
}
