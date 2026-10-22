// SPDX-FileCopyrightText: 2023 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package cc.tweaked.web.stub;

/**
 * A no-op stub for {@link java.util.concurrent.locks.ReentrantLock}.
 */
public final class ReentrantLock {
    private int isLocked;

    public void lock() {
        isLocked++;
    }

    public boolean tryLock() {
        isLocked++;
        return true;
    }

    public void lockInterruptibly() {
        isLocked++;
    }

    public void unlock() {
        if (isLocked == 0) throw new IllegalMonitorStateException("Lock is not locked");
        isLocked--;
    }

    public boolean isHeldByCurrentThread() {
        return isLocked != 0;
    }
}
