// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.test.core.filesystem;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;

import static org.junit.jupiter.api.Assertions.assertEquals;

public interface ReadableChannelContract {
    SeekableByteChannel wrap(byte[] contents);

    @Test
    default void testReadAdvancesPosition() throws IOException {
        try (var buffer = wrap(new byte[]{ 1, 2, 3, 4 })) {
            var read = ByteBuffer.allocate(2);

            assertEquals(2, buffer.read(read));
            assertEquals(2, read.position());
            assertEquals(2, buffer.position());
        }
    }

    @Test
    default void testAtOffsetReadAdvancesPosition() throws IOException {
        try (var buffer = wrap(new byte[]{ 1, 2, 3, 4 })) {
            buffer.position(2);
            var read = ByteBuffer.allocate(2);

            assertEquals(2, buffer.read(read));
            assertEquals(2, read.position());
            assertEquals(4, buffer.position());
        }
    }

    @Test
    default void testReadAtOffset() throws IOException {
        try (var buffer = wrap(new byte[]{ 1, 2, 3, 4 })) {
            buffer.position(2);
            var read = ByteBuffer.allocate(2);

            assertEquals(2, buffer.read(read));
            read.flip();
            assertEquals(3, read.get(0));
            assertEquals(4, read.get(1));
        }
    }
}
