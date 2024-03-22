// SPDX-FileCopyrightText: 2023 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.command.arguments;

import com.mojang.brigadier.StringReader;

final class ArgumentParserUtils {
    private ArgumentParserUtils() {
    }

    public static boolean consume(StringReader reader, char lookahead) {
        if (!reader.canRead() || reader.peek() != lookahead) return false;

        reader.skip();
        return true;
    }

    public static boolean consume(StringReader reader, String lookahead) {
        if (!reader.canRead(lookahead.length())) return false;
        for (var i = 0; i < lookahead.length(); i++) {
            if (reader.peek(i) != lookahead.charAt(i)) return false;
        }

        reader.setCursor(reader.getCursor() + lookahead.length());
        return true;
    }
}
