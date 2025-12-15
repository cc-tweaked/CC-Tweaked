// SPDX-FileCopyrightText: 2025 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.core.util;

import dan200.computercraft.api.lua.LuaValues;
import dan200.computercraft.test.core.ReplaceUnderscoresDisplayNameGenerator;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayNameGeneration(ReplaceUnderscoresDisplayNameGenerator.class)
class StringUtilTest {
    @ParameterizedTest
    @ValueSource(strings = { "hello\nworld", "hello\n\rworld", "hello\rworld" })
    public void getClipboardString_returns_a_single_line(String input) {
        var result = StringUtil.getClipboardString(input);
        assertEquals(LuaValues.encode("hello"), result);
    }

    @Test
    public void getClipboardString_limits_length() {
        var input = "abcdefghijklmnop".repeat(50);
        var result = StringUtil.getClipboardString(input);
        assertEquals(StringUtil.MAX_PASTE_LENGTH, result.limit());

        assertEquals(
            LuaValues.encode(input.substring(0, StringUtil.MAX_PASTE_LENGTH)),
            result
        );
    }
}
