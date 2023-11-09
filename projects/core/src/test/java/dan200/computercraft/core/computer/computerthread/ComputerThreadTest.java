// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.core.computer.computerthread;

import dan200.computercraft.core.computer.TimeoutState;
import dan200.computercraft.core.metrics.Metrics;
import dan200.computercraft.core.metrics.ThreadAllocations;
import dan200.computercraft.test.core.ConcurrentHelpers;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

@Timeout(value = 15)
@Execution(ExecutionMode.CONCURRENT)
public class ComputerThreadTest {
    private static final Logger LOG = LoggerFactory.getLogger(ComputerThreadTest.class);
    private ComputerThreadRunner manager;

    @BeforeEach
    public void before() {
        manager = new ComputerThreadRunner();
    }

    @AfterEach
    public void after() {
        manager.close();
    }

    @Test
    public void testSoftAbort() throws Exception {
        var computer = manager.createWorker((executor, timeout) -> {
            executor.setRemainingTime(TimeoutState.TIMEOUT);
            assertFalse(timeout.isSoftAborted(), "Should not start soft-aborted");

            var delay = ConcurrentHelpers.waitUntil(timeout::isSoftAborted);
            assertThat("Should be soft aborted", delay * 1e-9, closeTo(7, 1.0));
            LOG.info("Slept for {}", delay);
        });

        manager.startAndWait(computer);
    }

    @Test
    public void testHardAbort() throws Exception {
        var computer = manager.createWorker((executor, timeout) -> {
            executor.setRemainingTime(TimeoutState.TIMEOUT);
            assertFalse(timeout.isHardAborted(), "Should not start soft-aborted");

            assertThrows(InterruptedException.class, () -> Thread.sleep(11_000), "Sleep should be hard aborted");
            assertTrue(timeout.isHardAborted(), "Thread should be hard aborted");
        });

        manager.startAndWait(computer);
    }

    @Test
    public void testNoPauseIfNoOtherMachines() throws Exception {
        var computer = manager.createWorker((executor, timeout) -> {
            var didPause = ConcurrentHelpers.waitUntil(timeout::isPaused, 5, TimeUnit.SECONDS);
            assertFalse(didPause, "Machine shouldn't have paused within 5s");
        });

        manager.startAndWait(computer);
    }

    @Test
    public void testPauseIfSomeOtherMachine() throws Exception {
        var computer = manager.createWorker((executor, timeout) -> {
            var budget = manager.thread().scaledPeriod();
            assertEquals(budget, TimeUnit.MILLISECONDS.toNanos(25), "Budget should be 25ms");

            var delay = ConcurrentHelpers.waitUntil(timeout::isPaused);
            // Linux appears to have much more accurate timing than Windows/OSX. Or at least on CI!
            var time = System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("linux") ? 0.05 : 0.3;
            assertThat("Paused within a short time", delay * 1e-9, lessThanOrEqualTo(time));
        });

        manager.createLoopingComputer();

        manager.startAndWait(computer);
    }

    @Test
    public void testAllocationTracking() throws Exception {
        Assumptions.assumeTrue(ThreadAllocations.isSupported(), "Allocation tracking is supported");

        var size = 1024 * 1024 * 64;
        var computer = manager.createWorker((executor, timeout) -> {
            // Allocate some slab of memory. We try to blackhole the allocated object, but it's pretty naive
            // so who knows how useful it'll be.
            assertNotEquals(0, Objects.toString(new byte[size]).length());
        });
        manager.startAndWait(computer);

        assertThat(computer.getMetric(Metrics.JAVA_ALLOCATION), allOf(
            greaterThan((long) size), lessThan((long) (size + (size >> 2)))
        ));
    }
}
