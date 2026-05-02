// SPDX-FileCopyrightText: 2026 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.core.lua;

import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaTable;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Test Interface defining the behaviour of a {@link LuaTable} implementation.
 *
 * @param <T> The implementation of {@link LuaTable} we are testing.
 */
public interface LuaTableContract<T extends LuaTable<?, ?>> {
    T create(Map<?, ?> map);

    default T createList(List<?> list) {
        var out = new HashMap<>();
        var i = 0;
        for (var elem : list) {
            var idx = ++i;
            // We normalise our index to be a double, to match the behaviour of CobaltLuaMachine.toValue, which converts
            // all numbers to doubles.
            if (elem != null) out.put((double) idx, elem);
        }
        return create(out);
    }

    @Test
    default void testLength() {
        assertEquals(0, createList(List.of()).length());
        assertEquals(1, createList(List.of("a")).length());
        assertEquals(2, createList(List.of("a", "a")).length());
        assertEquals(1, createList(Arrays.asList("a", null, "a")).length());
    }

    @Test
    default void testGetIntLikeKey() {
        assertEquals("a", createList(List.of("a", "b", "c")).get(1));
        assertEquals("a", createList(List.of("a", "b", "c")).get(1.0));
        // This is a little dubious, but ensures we have consistent behaviour between implementations (doubles are
        // the only number) and we don't need to handle double/int normalisation within ObjectLuaTable.
        assertNull(createList(List.of("a", "b", "c")).get((Object) 1));
        assertNull(createList(List.of("a", "b", "c")).get(1.0f));
        assertNull(createList(List.of("a", "b", "c")).get((Object) (short) 1));
    }

    @Test
    default void testGetInt() throws LuaException {
        assertEquals("a", createList(List.of("a")).get(1));
        assertEquals(true, createList(List.of(true)).getBoolean(1));
        assertEquals(12345, createList(List.of(12345.0)).getInt(1));
        assertEquals(12345L, createList(List.of(12345.0)).getLong(1));
        assertEquals("abc", createList(List.of("abc")).getString(1));
    }
}
