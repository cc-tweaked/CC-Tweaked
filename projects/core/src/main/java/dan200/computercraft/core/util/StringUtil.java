// SPDX-FileCopyrightText: 2017 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.core.util;

import dan200.computercraft.core.input.ComputerInput;

import java.nio.ByteBuffer;

public final class StringUtil {
    public static final int MAX_PASTE_LENGTH = 512;

    private StringUtil() {
    }

    /**
     * Convert a Unicode character to a terminal one.
     *
     * @param chr The Unicode character.
     * @return The terminal character. This is either in the range [0, 255] (if a valid character) or {@code -1} if
     * it cannot be mapped to CC's charset.
     */
    public static int unicodeToTerminal(int chr) {
        // ASCII and latin1 map to themselves
        if (chr == 0 || chr == '\t' || chr == '\n' || chr == '\r' || (chr >= ' ' && chr <= '~') || (chr >= 160 && chr <= 255)) {
            return chr;
        }

        // Teletext block mosaics are *fairly* contiguous.
        if (chr >= 0x1FB00 && chr <= 0x1FB13) return chr + (129 - 0x1fb00);
        if (chr >= 0x1FB14 && chr <= 0x1FB1D) return chr + (150 - 0x1fb14);

        // Everything else is just a manual lookup. For now, we just use a big switch statement, which we spin into a
        // separate function to hopefully avoid inlining it here.
        return unicodeToCraftOsFallback(chr);
    }

    private static int unicodeToCraftOsFallback(int c) {
        return switch (c) {
            case 0x263A -> 1;
            case 0x263B -> 2;
            case 0x2665 -> 3;
            case 0x2666 -> 4;
            case 0x2663 -> 5;
            case 0x2660 -> 6;
            case 0x2022 -> 7;
            case 0x25D8 -> 8;
            case 0x2642 -> 11;
            case 0x2640 -> 12;
            case 0x266A -> 14;
            case 0x266B -> 15;
            case 0x25BA -> 16;
            case 0x25C4 -> 17;
            case 0x2195 -> 18;
            case 0x203C -> 19;
            case 0x25AC -> 22;
            case 0x21A8 -> 23;
            case 0x2191 -> 24;
            case 0x2193 -> 25;
            case 0x2192 -> 26;
            case 0x2190 -> 27;
            case 0x221F -> 28;
            case 0x2194 -> 29;
            case 0x25B2 -> 30;
            case 0x25BC -> 31;
            case 0x1FB99 -> 127;
            case 0x258C -> 149;
            default -> -1;
        };
    }

    /**
     * Check if a character is capable of being input and passed to a {@linkplain ComputerInput#charTyped(byte) "char"
     * event}.
     *
     * @param chr The character to check.
     * @return Whether this character can be typed.
     */
    public static boolean isTypableChar(byte chr) {
        return isTypableChar(chr & 0xFF);
    }

    /**
     * Check if a character is capable of being input and passed to a {@linkplain ComputerInput#charTyped(byte) "char"
     * * event}.
     *
     * @param chr The character to check.
     * @return Whether this character can be typed.
     */
    public static boolean isTypableChar(int chr) {
        return chr >= 0 && chr <= 255 && chr != 0 && chr != '\r' && chr != '\n';
    }

    private static boolean isAllowedInLabel(char c) {
        // Limit to ASCII and latin1, excluding '§' (Minecraft's formatting character).
        return (c >= ' ' && c <= '~') || (c >= 161 && c <= 255 && c != 167);
    }

    public static String normaliseLabel(String text) {
        var length = Math.min(32, text.length());
        var builder = new StringBuilder(length);
        for (var i = 0; i < length; i++) {
            var c = text.charAt(i);
            builder.append(isAllowedInLabel(c) ? c : '?');
        }
        return builder.toString();
    }

    /**
     * Convert a Java string to a Lua one (using the terminal charset), suitable for pasting into a computer.
     * <p>
     * This removes special characters and strips to the first line of text.
     *
     * @param clipboard The text from the clipboard.
     * @return The encoded clipboard text.
     */
    public static ByteBuffer getClipboardString(String clipboard) {
        var output = new byte[Math.min(MAX_PASTE_LENGTH, clipboard.length())];
        var idx = 0;

        var iterator = clipboard.codePoints().iterator();
        while (iterator.hasNext() && idx < output.length) {
            var chr = unicodeToTerminal(iterator.next());
            if (chr < 0) continue; // Strip out unconvertible characters
            if (!isTypableChar(chr)) break; // Stop at untypable ones.
            output[idx++] = (byte) chr;
        }

        return ByteBuffer.wrap(output, 0, idx).asReadOnlyBuffer();
    }
}
