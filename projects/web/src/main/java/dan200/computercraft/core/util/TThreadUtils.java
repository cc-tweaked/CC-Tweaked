// SPDX-FileCopyrightText: 2026 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.core.util;

import com.google.common.util.concurrent.CycleDetectingLockFactory;

import java.util.concurrent.locks.ReentrantLock;

/**
 * Replacement for {@link ThreadUtils} that does not use {@link CycleDetectingLockFactory}.
 */
public class TThreadUtils {
    @SuppressWarnings("ForbiddenAPI")
    public static ReentrantLock newReentrantLock(String name) {
        return new ReentrantLock();
    }
}
