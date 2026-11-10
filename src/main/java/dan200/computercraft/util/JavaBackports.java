// SPDX-FileCopyrightText: 2026 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.util;

import org.jspecify.annotations.Nullable;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.IntFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class JavaBackports {
    private static int mismatchImpl(byte[] a, int aStart, byte[] a2, int a2Start, int length) {
        for (int i = 0; i < length; ++i) {
            if (a[i + aStart] != a2[i + a2Start]) {
                return i;
            }
        }
        return -1;
    }

    public static int mismatch(byte[] a, int aFromIndex, int aToIndex, byte[] b, int bFromIndex, int bToIndex) {
        checkInBounds(a.length, aFromIndex, aToIndex);
        checkInBounds(b.length, bFromIndex, bToIndex);

        int aLength = aToIndex - aFromIndex;
        int bLength = bToIndex - bFromIndex;
        int length = Math.min(aLength, bLength);
        int mismatch = mismatchImpl(a, aFromIndex, b, bFromIndex, length);
        return mismatch < 0 && aLength != bLength ? length : mismatch;
    }

    public static boolean equals(byte[] a, int aFromIndex, int aToIndex, byte[] b, int bFromIndex, int bToIndex) {
        checkInBounds(a.length, aFromIndex, aToIndex);
        checkInBounds(b.length, bFromIndex, bToIndex);

        int aLength = aToIndex - aFromIndex;
        int bLength = bToIndex - bFromIndex;
        return aLength == bLength && mismatchImpl(a, aFromIndex, b, bFromIndex, aLength) < 0;
    }

    public static int compareUnsigned(byte[] a, byte[] b) {
        if (a == b) {
            return 0;
        } else if (a != null && b != null) {
            var i = mismatchImpl(a, 0, b, 0, Math.min(a.length, b.length));
            return i >= 0 ? Byte.compareUnsigned(a[i], b[i]) : a.length - b.length;
        } else {
            return a == null ? -1 : 1;
        }
    }

    private static void checkInBounds(int length, int startInclusive, int endExclusive) {
        if (startInclusive < 0 || endExclusive < startInclusive || endExclusive > length) {
            throw new ArrayIndexOutOfBoundsException();
        }
    }

    public static int compareUnsigned(byte a, byte b) {
        return (a & 255) - (b & 255);
    }

    public static <T> T requireNonNullElse(@Nullable T instance, T replacement) {
        return instance == null ? replacement : instance;
    }

    public static <T> T[] toArray(List<T> list, IntFunction<T[]> newArray) {
        var array = newArray.apply(list.size());
        list.toArray(array);
        return array;
    }

    public static <T> List<T> toList(Stream<T> stream) {
        return Collections.unmodifiableList(stream.collect(Collectors.toList()));
    }

    public static <T> List<T> listOf(T a, T b) {
        return Collections.unmodifiableList(Arrays.asList(a, b));
    }
}
