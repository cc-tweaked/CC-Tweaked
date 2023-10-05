// SPDX-FileCopyrightText: 2023 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package io.netty.util;

import org.teavm.interop.NoSideEffects;

/**
 * A replacement for {@link AsciiString} which just wraps a normal string.
 * <p>
 * {@link AsciiString} relies heavily on Netty's low-level (and often unsafe!) code, which doesn't run on Javascript.
 */
public final class TAsciiString implements CharSequence {
    private final String value;

    private TAsciiString(String value) {
        this.value = value;
    }

    @NoSideEffects
    public static TAsciiString cached(String value) {
        return new TAsciiString(value);
    }

    @Override
    public int length() {
        return value.length();
    }

    @Override
    public char charAt(int index) {
        return value.charAt(index);
    }

    @Override
    public CharSequence subSequence(int start, int end) {
        return value.subSequence(start, end);
    }

    @Override
    public String toString() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        return this == o || (o instanceof TAsciiString other && value.equals(other.value));
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }
}
