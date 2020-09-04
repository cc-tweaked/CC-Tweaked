/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.core.asm;

import java.util.Arrays;
import java.util.function.IntFunction;

public final class IntCache<T> {
    private final IntFunction<T> factory;
    private volatile Object[] cache = new Object[16];

    IntCache(IntFunction<T> factory) {
        this.factory = factory;
    }

    @SuppressWarnings ("unchecked")
    public T get(int index) {
        if (index < 0) {
            throw new IllegalArgumentException("index < 0");
        }

        if (index < this.cache.length) {
            T current = (T) this.cache[index];
            if (current != null) {
                return current;
            }
        }

        synchronized (this) {
            if (index >= this.cache.length) {
                this.cache = Arrays.copyOf(this.cache, Math.max(this.cache.length * 2, index + 1));
            }
            T current = (T) this.cache[index];
            if (current == null) {
                this.cache[index] = current = this.factory.apply(index);
            }
            return current;
        }
    }
}
