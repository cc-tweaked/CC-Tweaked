// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.client.sound;

import io.netty.buffer.ByteBufAllocator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DfpwmStreamTest {
    @Test
    public void testDecodesBytes() {
        var stream = new DfpwmStream();

        var input = ByteBufAllocator.DEFAULT.buffer();
        input.writeBytes(new byte[]{ 43, -31, 33, 44, 30, -16, -85, 23, -3, -55, 46, -70, 68, -67, 74, -96, -68, 16, 94, -87, -5, 87, 11, -16, 19, 92, 85, -71, 126, 5, -84, 64, 17, -6, 85, -11, -1, -87, -12, 1, 85, -56, 33, -80, 82, 104, -93, 17, 126, 23, 91, -30, 37, -32, 117, -72, -58, 11, -76, 19, -108, 86, -65, -10, -1, -68, -25, 10, -46, 85, 124, -54, 15, -24, 43, -94, 117, 63, -36, 15, -6, 88, 87, -26, -83, 106, 41, 13, -28, -113, -10, -66, 119, -87, -113, 68, -55, 40, -107, 62, 20, 72, 3, -96, 114, -87, -2, 39, -104, 30, 20, 42, 84, 24, 47, 64, 43, 61, -35, 95, -65, 42, 61, 42, -50, 4, -9, 81 });
        stream.push(input, false);

        var buffer = stream.read(1024 + 1);
        assertEquals(1024, buffer.remaining(), "Must have read 1024 bytes");

        var decoded = new byte[]{ 1, 2, 2, 2, 2, 2, 2, 1, 1, 1, 0, -1, -2, -2, -1, 0, 1, 0, -1, -3, -5, -5, -5, -7, -9, -11, -11, -9, -9, -9, -9, -10, -12, -12, -10, -8, -6, -6, -8, -10, -12, -14, -16, -18, -17, -15, -12, -9, -6, -3, -2, -2, -2, -2, -2, -2, 0, 3, 6, 7, 7, 7, 4, 1, 1, 1, 1, 3, 5, 7, 9, 12, 15, 15, 12, 12, 12, 9, 9, 11, 12, 12, 14, 16, 17, 17, 17, 14, 11, 11, 11, 10, 12, 14, 14, 13, 13, 10, 9, 9, 7, 5, 4, 4, 4, 4, 4, 6, 8, 10, 10, 10, 10, 10, 10, 10, 9, 8, 8, 8, 7, 6, 4, 2, 0, 0, 0, 0, 0, -1, -1, 0, 1, 3, 3, 3, 3, 2, 0, -2, -2, -2, -3, -5, -7, -7, -5, -3, -1, -1, -1, -1, -1, -1, -2, -2, -1, -1, -1, -1, 0, 1, 1, 1, 2, 3, 4, 5, 6, 7, 9, 9, 9, 9, 9, 9, 9, 10, 10, 10, 10, 9, 8, 7, 6, 4, 2, 0, 0, 2, 4, 6, 8, 10, 10, 8, 7, 7, 5, 3, 1, -1, 0, 2, 4, 5, 5, 5, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 3, 3, 4, 5, 5, 5, 5, 5, 6, 7, 8, 9, 10, 9, 9, 9, 9, 9, 8, 7, 6, 5, 3, 1, 1, 3, 3, 3, 3, 3, 3, 2, 1, 0, -1, -3, -3, -3, -3, -2, -3, -4, -4, -3, -4, -5, -6, -6, -5, -5, -4, -3, -2, 0, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 3, 4, 5, 6, 7, 8, 10, 12, 14, 16, 18, 20, 20, 17, 16, 16, 15, 15, 15, 15, 13, 13, 13, 13, 14, 15, 16, 18, 18, 16, 14, 12, 10, 8, 5, 5, 5, 4, 4, 4, 4, 4, 4, 2, 0, -2, -2, -2, -4, -4, -2, 0, 0, -2, -4, -6, -6, -6, -8, -10, -12, -14, -16, -15, -13, -12, -11, -11, -11, -11, -13, -13, -13, -13, -13, -14, -16, -18, -18, -18, -18, -16, -16, -16, -14, -13, -14, -15, -15, -14, -14, -12, -11, -12, -13, -13, -12, -13, -14, -15, -15, -13, -11, -9, -7, -5, -5, -5, -3, -1, -1, -1, -1, -3, -5, -5, -3, -3, -3, -1, -1, -1, -1, -3, -3, -3, -4, -6, -6, -4, -2, 0, 0, 0, 0, -2, -2, -2, -3, -5, -7, -9, -11, -13, -13, -11, -9, -7, -6, -6, -6, -6, -4, -2, -2, -4, -6, -8, -7, -5, -3, -2, -2, -2, -2, 0, 0, -2, -4, -4, -2, 0, 2, 2, 1, 1, -1, -3, -5, -7, -10, -10, -10, -10, -8, -7, -7, -5, -3, -2, -4, -4, -4, -6, -8, -10, -12, -12, -12, -12, -12, -14, -13, -13, -13, -11, -11, -11, -11, -11, -11, -11, -9, -7, -5, -3, -1, -1, -1, -1, -1, 1, 1, 1, 2, 4, 6, 8, 10, 12, 14, 16, 18, 20, 22, 22, 19, 18, 20, 22, 24, 23, 22, 24, 26, 28, 27, 24, 23, 25, 28, 28, 28, 27, 26, 26, 23, 20, 17, 14, 14, 14, 11, 11, 11, 11, 13, 15, 16, 16, 16, 15, 15, 14, 14, 12, 10, 9, 11, 13, 15, 17, 17, 14, 13, 13, 12, 12, 10, 9, 11, 13, 15, 17, 19, 19, 16, 13, 10, 7, 4, 1, 1, 2, 2, 4, 7, 10, 13, 13, 13, 12, 12, 12, 9, 6, 6, 6, 3, 0, 0, 0, 0, 2, 3, 3, 3, 3, 5, 7, 7, 7, 9, 11, 13, 15, 18, 18, 15, 12, 9, 8, 10, 13, 13, 13, 15, 18, 21, 24, 27, 27, 23, 19, 15, 11, 10, 9, 9, 12, 16, 19, 22, 23, 19, 14, 13, 16, 16, 15, 15, 14, 17, 20, 20, 19, 19, 18, 17, 14, 13, 15, 15, 12, 11, 13, 16, 19, 19, 18, 20, 20, 19, 18, 18, 17, 17, 16, 16, 16, 15, 17, 17, 16, 16, 13, 12, 12, 11, 11, 9, 9, 9, 9, 11, 11, 9, 7, 5, 3, 1, 1, 1, -1, -1, 1, 3, 5, 7, 9, 11, 12, 9, 6, 6, 6, 6, 8, 8, 7, 9, 11, 13, 13, 12, 14, 16, 18, 20, 20, 20, 22, 24, 26, 25, 25, 27, 29, 28, 27, 26, 23, 22, 22, 21, 21, 20, 22, 24, 26, 28, 27, 24, 21, 21, 21, 18, 17, 17, 14, 11, 11, 11, 10, 10, 7, 6, 6, 4, 3, 5, 5, 3, 1, 1, 1, 1, 1, -1, -1, -1, -1, -1, -1, 0, -1, -1, 0, 0, 1, 2, 3, 4, 3, 1, -1, -3, -3, -3, -3, -2, -3, -4, -6, -8, -10, -10, -10, -12, -12, -12, -12, -10, -10, -11, -12, -14, -16, -18, -20, -22, -24, -26, -28, -27, -27, -26, -26, -25, -25, -27, -26, -24, -22, -22, -22, -22, -24, -24, -24, -24, -23, -23, -22, -22, -21, -20, -19, -17, -15, -13, -11, -9, -7, -7, -9, -9, -9, -11, -13, -15, -17, -16, -14, -13, -15, -14, -14, -14, -12, -10, -8, -7, -9, -11, -13, -15, -14, -14, -13, -13, -15, -17, -19, -18, -18, -17, -17, -16, -16, -18, -20, -22, -21, -21, -21, -21, -21, -20, -21, -22, -24, -24, -22, -22, -24, -26, -25, -23, -21, -19, -18, -17, -17, -19, -21, -23, -25, -27, -29, -31, -30, -29, -28, -26, -25, -24, -24, -23, -23, -25, -24, -24, -24, -22, -20, -18, -18, -20, -20, -20, -20, -18, -16, -16, -16, -14, -12, -10, -8, -6, -4, -4, -4, -4, -4, -2, 0, 2, 4, 6, 6, 5, 5, 5, 5, 5, 5, 5, 5, 3, 3, 3, 3, 4, 5, 6, 5, 3, 1, 1, 1, 1, 1, 1, 1, 0, -1, -1, 0, 1, 1, 0, 0, 1, 1, 0, 0, 0, -1, -2, -3, -4, -4, -2, 0, 0, 0, 1, 3, 5, 7, 7, 5, 3, 3, 3, 3, 3 };
        for (var i = 0; i < 1024; i++) {
            assertEquals((byte) (decoded[i] ^ 0x80), buffer.get(), "Bad element at " + i);
        }

        assertEquals(0, buffer.remaining(), "Must have read all bytes");
    }
}
