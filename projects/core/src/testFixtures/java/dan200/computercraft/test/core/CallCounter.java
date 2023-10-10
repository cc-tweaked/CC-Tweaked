// SPDX-FileCopyrightText: 2021 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.test.core;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class CallCounter implements Runnable {
    private int timesCalled = 0;

    @Override
    public void run() {
        timesCalled++;
    }

    public void assertCalledTimes(int expectedTimesCalled) {
        assertEquals(expectedTimesCalled, timesCalled, "Callback was not called the correct number of times");
    }

    public void assertNotCalled() {
        assertEquals(0, timesCalled, "Should never have been called.");
    }

    public void reset() {
        this.timesCalled = 0;
    }
}
