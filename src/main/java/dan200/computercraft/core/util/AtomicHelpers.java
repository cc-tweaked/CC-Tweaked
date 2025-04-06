// SPDX-FileCopyrightText: 2023 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.core.util;

import java.util.concurrent.atomic.AtomicInteger;

public final class AtomicHelpers {
    private AtomicHelpers() {
    }

    /**
     * A version of {@link AtomicInteger#getAndIncrement()}, which increments until a limit is reached.
     *
     * @param atomic The atomic to increment.
     * @param limit  The maximum value of {@code value}.
     * @return Whether the value was sucessfully incremented.
     */
    public static boolean incrementToLimit(AtomicInteger atomic, int limit) {
        int value;
        do {
            value = atomic.get();
            if (value >= limit) return false;
        } while (!atomic.compareAndSet(value, value + 1));

        return true;
    }
}
