// SPDX-FileCopyrightText: 2023 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.util;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * A couple of trivial tests for {@link ConsList}, mostly as a quick safety check.
 */
public class ConsListTest {
    @Test
    public void testGet() {
        var list = new ConsList<>(1, List.of(2, 3, 4));
        assertEquals(1, list.get(0));
        assertEquals(2, list.get(1));
        assertEquals(4, list.get(3));
    }

    @Test
    public void testSize() {
        var list = new ConsList<>(1, List.of(2, 3, 4));
        assertEquals(4, list.size());
    }

    @Test
    public void testIterator() {
        var list = new ConsList<>(1, List.of(2, 3, 4));
        assertArrayEquals(new Integer[]{ 1, 2, 3, 4 }, list.toArray(Integer[]::new));
    }
}
