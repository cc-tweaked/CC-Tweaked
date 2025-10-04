// SPDX-FileCopyrightText: 2023 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package io.netty.util;

import org.teavm.interop.NoSideEffects;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * A replacement for {@link AsciiString} which just wraps a normal string.
 * <p>
 * {@link AsciiString} relies heavily on {@link String#String(byte[], int, int, int)}, which isn't supported in TeaVM.
 */
public final class TAsciiString implements CharSequence {
    private String string;
    private final byte[] array;
    private final int offset, length;

    private TAsciiString(String value) {
        string = value;
        array = value.getBytes(StandardCharsets.UTF_8);
        this.offset = 0;
        this.length = array.length;
    }

    private TAsciiString(byte[] array, int offset, int length) {
        this.array = array;
        this.offset = offset;
        this.length = length;
    }

    @NoSideEffects
    public static TAsciiString cached(String value) {
        return new TAsciiString(value);
    }

    @NoSideEffects
    public static TAsciiString of(CharSequence value) {
        return value instanceof TAsciiString str ? str : new TAsciiString(value.toString());
    }

    public byte[] array() {
        return array;
    }

    public int arrayOffset() {
        return offset;
    }

    @Override
    public int length() {
        return length;
    }

    @Override
    public char charAt(int index) {
        return (char) (array[offset + index] & 0xFF);
    }

    @Override
    public CharSequence subSequence(int start, int end) {
        return new TAsciiString(array, start + offset, end + offset);
    }

    @Override
    public String toString() {
        return string != null ? string : (string = new String(array, offset, length, StandardCharsets.UTF_8));
    }

    @Override
    public boolean equals(Object o) {
        return this == o || (o instanceof TAsciiString other && Arrays.equals(array, offset, offset + length, other.array, other.offset, other.offset + other.length));
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(array);
    }
}
