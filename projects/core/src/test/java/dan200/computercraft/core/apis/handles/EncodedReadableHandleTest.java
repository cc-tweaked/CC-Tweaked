// SPDX-FileCopyrightText: 2018 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.core.apis.handles;

import dan200.computercraft.api.lua.LuaException;
import org.junit.jupiter.api.Test;

import javax.annotation.Nullable;
import java.io.BufferedReader;
import java.io.CharArrayReader;
import java.util.Arrays;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class EncodedReadableHandleTest {
    @Test
    public void testReadChar() throws LuaException {
        var handle = fromLength(5);
        assertEquals("A", cast(String.class, handle.read(Optional.empty())));
    }

    @Test
    public void testReadShortComplete() throws LuaException {
        var handle = fromLength(10);
        assertEquals("AAAAA", cast(String.class, handle.read(Optional.of(5))));
    }

    @Test
    public void testReadShortPartial() throws LuaException {
        var handle = fromLength(5);
        assertEquals("AAAAA", cast(String.class, handle.read(Optional.of(10))));
    }


    @Test
    public void testReadLongComplete() throws LuaException {
        var handle = fromLength(10000);
        assertEquals(9000, cast(String.class, handle.read(Optional.of(9000))).length());
    }

    @Test
    public void testReadLongPartial() throws LuaException {
        var handle = fromLength(10000);
        assertEquals(10000, cast(String.class, handle.read(Optional.of(11000))).length());
    }

    @Test
    public void testReadLongPartialSmaller() throws LuaException {
        var handle = fromLength(1000);
        assertEquals(1000, cast(String.class, handle.read(Optional.of(11000))).length());
    }

    private static EncodedReadableHandle fromLength(int length) {
        var input = new char[length];
        Arrays.fill(input, 'A');
        return new EncodedReadableHandle(new BufferedReader(new CharArrayReader(input)));
    }

    private static <T> T cast(Class<T> type, @Nullable Object[] values) {
        if (values == null || values.length < 1) throw new NullPointerException();
        return type.cast(values[0]);
    }
}
