// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.core.computer;

import dan200.computercraft.core.lua.MachineResult;
import dan200.computercraft.test.core.ConcurrentHelpers;
import dan200.computercraft.test.core.computer.KotlinComputerManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.junit.jupiter.api.Assertions.*;

@Timeout(value = 15)
@Execution(ExecutionMode.CONCURRENT)
public class ComputerThreadTest {
    private static final Logger LOG = LoggerFactory.getLogger(ComputerThreadTest.class);
    private KotlinComputerManager manager;

    @BeforeEach
    public void before() {
        manager = new KotlinComputerManager();
    }

    @AfterEach
    public void after() {
        manager.close();
    }

    @Test
    public void testSoftAbort() throws Exception {
        var computer = manager.create();
        manager.enqueue(computer, timeout -> {
            assertFalse(timeout.isSoftAborted(), "Should not start soft-aborted");

            var delay = ConcurrentHelpers.waitUntil(timeout::isSoftAborted);
            assertThat("Should be soft aborted", delay * 1e-9, closeTo(7, 1.0));
            LOG.info("Slept for {}", delay);

            computer.shutdown();
            return MachineResult.OK;
        });

        manager.startAndWait(computer);
    }

    @Test
    public void testHardAbort() throws Exception {
        var computer = manager.create();
        manager.enqueue(computer, timeout -> {
            assertFalse(timeout.isHardAborted(), "Should not start soft-aborted");

            assertThrows(InterruptedException.class, () -> Thread.sleep(11_000), "Sleep should be hard aborted");
            assertTrue(timeout.isHardAborted(), "Thread should be hard aborted");

            computer.shutdown();
            return MachineResult.OK;
        });

        manager.startAndWait(computer);
    }

    @Test
    public void testNoPauseIfNoOtherMachines() throws Exception {
        var computer = manager.create();
        manager.enqueue(computer, timeout -> {
            var didPause = ConcurrentHelpers.waitUntil(timeout::isPaused, 5, TimeUnit.SECONDS);
            assertFalse(didPause, "Machine shouldn't have paused within 5s");

            computer.shutdown();
            return MachineResult.OK;
        });

        manager.startAndWait(computer);
    }

    @Test
    public void testPauseIfSomeOtherMachine() throws Exception {
        var computer = manager.create();
        manager.enqueue(computer, timeout -> {
            var budget = manager.context().computerScheduler().scaledPeriod();
            assertEquals(budget, TimeUnit.MILLISECONDS.toNanos(25), "Budget should be 25ms");

            var delay = ConcurrentHelpers.waitUntil(timeout::isPaused);
            // Linux appears to have much more accurate timing than Windows/OSX. Or at least on CI!
            var time = System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("linux") ? 0.05 : 0.3;
            assertThat("Paused within a short time", delay * 1e-9, lessThanOrEqualTo(time));

            computer.shutdown();
            return MachineResult.OK;
        });

        manager.createLoopingComputer();

        manager.startAndWait(computer);
    }
}
