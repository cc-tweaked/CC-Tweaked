// SPDX-FileCopyrightText: 2020 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.core.asm;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class IntCacheTest {
    @Test
    public void testCache() {
        var c = new IntCache<Object>(i -> new Object());
        assertEquals(c.get(0), c.get(0));
    }

    @Test
    public void testMassive() {
        assertEquals(40, new IntCache<>(i -> i).get(40));
    }
}
