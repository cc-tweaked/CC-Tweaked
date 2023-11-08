// SPDX-FileCopyrightText: 2018 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.core.apis.handles;

import dan200.computercraft.api.lua.LuaException;
import org.junit.jupiter.api.Test;

import javax.annotation.Nullable;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class BinaryReadableHandleTest {
    @Test
    public void testReadChar() throws LuaException {
        var handle = fromLength(5);
        assertEquals('A', cast(Integer.class, handle.read(Optional.empty())));
    }

    @Test
    public void testReadShortComplete() throws LuaException {
        var handle = fromLength(10);
        assertEquals(5, cast(ByteBuffer.class, handle.read(Optional.of(5))).remaining());
    }

    @Test
    public void testReadShortPartial() throws LuaException {
        var handle = fromLength(5);
        assertEquals(5, cast(ByteBuffer.class, handle.read(Optional.of(10))).remaining());
    }

    @Test
    public void testReadLongComplete() throws LuaException {
        var handle = fromLength(10000);
        assertEquals(9000, cast(byte[].class, handle.read(Optional.of(9000))).length);
    }

    @Test
    public void testReadLongPartial() throws LuaException {
        var handle = fromLength(10000);
        assertEquals(10000, cast(byte[].class, handle.read(Optional.of(11000))).length);
    }

    @Test
    public void testReadLongPartialSmaller() throws LuaException {
        var handle = fromLength(1000);
        assertEquals(1000, cast(ByteBuffer.class, handle.read(Optional.of(11000))).remaining());
    }

    @Test
    public void testReadLine() throws LuaException {
        var handle = new ReadHandle(new ArrayByteChannel("hello\r\nworld\r!".getBytes(StandardCharsets.UTF_8)), false);
        assertArrayEquals("hello".getBytes(StandardCharsets.UTF_8), cast(byte[].class, handle.readLine(Optional.empty())));
        assertArrayEquals("world\r!".getBytes(StandardCharsets.UTF_8), cast(byte[].class, handle.readLine(Optional.empty())));
        assertNull(handle.readLine(Optional.empty()));
    }

    @Test
    public void testReadLineTrailing() throws LuaException {
        var handle = new ReadHandle(new ArrayByteChannel("hello\r\nworld\r!".getBytes(StandardCharsets.UTF_8)), false);
        assertArrayEquals("hello\r\n".getBytes(StandardCharsets.UTF_8), cast(byte[].class, handle.readLine(Optional.of(true))));
        assertArrayEquals("world\r!".getBytes(StandardCharsets.UTF_8), cast(byte[].class, handle.readLine(Optional.of(true))));
        assertNull(handle.readLine(Optional.of(true)));
    }

    private static ReadHandle fromLength(int length) {
        var input = new byte[length];
        Arrays.fill(input, (byte) 'A');
        return new ReadHandle(new ArrayByteChannel(input), true);
    }

    private static <T> T cast(Class<T> type, @Nullable Object[] values) {
        if (values == null || values.length < 1) throw new NullPointerException();
        return type.cast(values[0]);
    }
}
