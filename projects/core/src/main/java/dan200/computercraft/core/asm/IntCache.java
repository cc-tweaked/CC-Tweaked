// SPDX-FileCopyrightText: 2020 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.core.asm;

import java.util.Arrays;
import java.util.function.IntFunction;

final class IntCache<T> {
    private final IntFunction<T> factory;
    private volatile Object[] cache = new Object[16];

    IntCache(IntFunction<T> factory) {
        this.factory = factory;
    }

    @SuppressWarnings("unchecked")
    public T get(int index) {
        if (index < 0) throw new IllegalArgumentException("index < 0");

        if (index < cache.length) {
            var current = (T) cache[index];
            if (current != null) return current;
        }

        synchronized (this) {
            if (index >= cache.length) cache = Arrays.copyOf(cache, Math.max(cache.length * 2, index + 1));
            var current = (T) cache[index];
            if (current == null) cache[index] = current = factory.apply(index);
            return current;
        }
    }
}
