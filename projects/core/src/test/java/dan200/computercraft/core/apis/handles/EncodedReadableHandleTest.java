// SPDX-FileCopyrightText: 2018 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.core.apis.handles;

import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.core.apis.ObjectWrapper;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.CharArrayReader;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class EncodedReadableHandleTest {
    @Test
    public void testReadChar() throws LuaException {
        var wrapper = fromLength(5);
        assertEquals("A", wrapper.callOf("read"));
    }

    @Test
    public void testReadShortComplete() throws LuaException {
        var wrapper = fromLength(10);
        assertEquals("AAAAA", wrapper.callOf("read", 5));
    }

    @Test
    public void testReadShortPartial() throws LuaException {
        var wrapper = fromLength(5);
        assertEquals("AAAAA", wrapper.callOf("read", 10));
    }


    @Test
    public void testReadLongComplete() throws LuaException {
        var wrapper = fromLength(10000);
        assertEquals(9000, wrapper.<String>callOf("read", 9000).length());
    }

    @Test
    public void testReadLongPartial() throws LuaException {
        var wrapper = fromLength(10000);
        assertEquals(10000, wrapper.<String>callOf("read", 11000).length());
    }

    @Test
    public void testReadLongPartialSmaller() throws LuaException {
        var wrapper = fromLength(1000);
        assertEquals(1000, wrapper.<String>callOf("read", 11000).length());
    }

    private static ObjectWrapper fromLength(int length) {
        var input = new char[length];
        Arrays.fill(input, 'A');
        return new ObjectWrapper(new EncodedReadableHandle(new BufferedReader(new CharArrayReader(input))));
    }
}
