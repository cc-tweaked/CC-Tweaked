// SPDX-FileCopyrightText: 2023 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package cc.tweaked.web.stub;

/**
 * A no-op stub for {@link java.util.concurrent.locks.ReentrantLock}.
 */
public class ReentrantLock {
    public void lock() {
    }

    public boolean tryLock() {
        return true;
    }

    public void unlock() {
    }

    public void lockInterruptibly() throws InterruptedException {
    }
}
