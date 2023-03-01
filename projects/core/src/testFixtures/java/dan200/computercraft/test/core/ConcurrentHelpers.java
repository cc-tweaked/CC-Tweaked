// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.test.core;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;
import java.util.function.BooleanSupplier;

/**
 * Utilities for working with concurrent systems.
 */
public class ConcurrentHelpers {
    private static final long DELAY = TimeUnit.MILLISECONDS.toNanos(2);

    /**
     * Wait until a condition is true, checking the condition every 2ms.
     *
     * @param isTrue The condition to check
     * @return How long we waited for.
     */
    public static long waitUntil(BooleanSupplier isTrue) {
        var start = System.nanoTime();
        while (true) {
            if (isTrue.getAsBoolean()) return System.nanoTime() - start;
            LockSupport.parkNanos(DELAY);
        }
    }

    /**
     * Wait until a condition is true or a timeout is elapsed, checking the condition every 2ms.
     *
     * @param isTrue  The condition to check
     * @param timeout The delay after which we will timeout.
     * @param unit    The time unit the duration is measured in.
     * @return {@literal true} if the condition was met, {@literal false} if we timed out instead.
     */
    public static boolean waitUntil(BooleanSupplier isTrue, long timeout, TimeUnit unit) {
        var start = System.nanoTime();
        var timeoutNs = unit.toNanos(timeout);
        while (true) {
            var time = System.nanoTime() - start;
            if (isTrue.getAsBoolean()) return true;
            if (time > timeoutNs) return false;

            LockSupport.parkNanos(DELAY);
        }
    }
}
