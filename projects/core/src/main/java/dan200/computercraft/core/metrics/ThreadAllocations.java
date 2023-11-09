// SPDX-FileCopyrightText: 2023 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.core.metrics;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;

/**
 * Provides a way to get the memory allocated by a specific thread.
 * <p>
 * This uses Hotspot-specific functionality, so may not be available on all JVMs. Consumers should call
 * {@link #isSupported()} before calling more specific methods.
 *
 * @see com.sun.management.ThreadMXBean
 */
public final class ThreadAllocations {
    private static final Logger LOG = LoggerFactory.getLogger(ThreadAllocations.class);

    private static final @Nullable MethodHandle threadAllocatedBytes;
    private static final @Nullable MethodHandle threadsAllocatedBytes;

    static {
        MethodHandle threadAllocatedBytesHandle, threadsAllocatedBytesHandle;
        try {
            var threadMxBean = Class.forName("com.sun.management.ThreadMXBean").asSubclass(ThreadMXBean.class);
            var bean = ManagementFactory.getPlatformMXBean(threadMxBean);

            // Enable allocation tracking.
            threadMxBean.getMethod("setThreadAllocatedMemoryEnabled", boolean.class).invoke(bean, true);

            // Just probe this method once to check it doesn't error.
            threadMxBean.getMethod("getCurrentThreadAllocatedBytes").invoke(bean);

            threadAllocatedBytesHandle = MethodHandles.publicLookup()
                .findVirtual(threadMxBean, "getThreadAllocatedBytes", MethodType.methodType(long.class, long.class))
                .bindTo(bean);
            threadsAllocatedBytesHandle = MethodHandles.publicLookup()
                .findVirtual(threadMxBean, "getThreadAllocatedBytes", MethodType.methodType(long[].class, long[].class))
                .bindTo(bean);
        } catch (LinkageError | ReflectiveOperationException | RuntimeException e) {
            LOG.warn("Cannot track allocated memory of computer threads", e);
            threadAllocatedBytesHandle = threadsAllocatedBytesHandle = null;
        }

        threadAllocatedBytes = threadAllocatedBytesHandle;
        threadsAllocatedBytes = threadsAllocatedBytesHandle;
    }

    private ThreadAllocations() {
    }

    /**
     * Check whether the current JVM provides information about per-thread allocations.
     *
     * @return Whether per-thread allocation information is available.
     */
    public static boolean isSupported() {
        return threadAllocatedBytes != null;
    }

    /**
     * Get an approximation the amount of memory a thread has allocated over its lifetime.
     *
     * @param threadId The ID of the thread.
     * @return The allocated memory, in bytes.
     * @see com.sun.management.ThreadMXBean#getThreadAllocatedBytes(long)
     */
    public static long getAllocatedBytes(long threadId) {
        if (threadAllocatedBytes == null) {
            throw new UnsupportedOperationException("Allocated bytes are not supported");
        }

        try {
            return (long) threadAllocatedBytes.invokeExact(threadId);
        } catch (Throwable t) {
            throw throwUnchecked0(t); // Should never occur, but if it does it's guaranteed to be a runtime exception.
        }
    }

    /**
     * Get an approximation the amount of memory a thread has allocated over its lifetime.
     * <p>
     * This is equivalent to calling {@link #getAllocatedBytes(long)} for each thread in {@code threadIds}.
     *
     * @param threadIds An array of thread IDs.
     * @return An array with the same length as {@code threadIds}, containing the allocated memory for each thread.
     * @see com.sun.management.ThreadMXBean#getThreadAllocatedBytes(long[])
     */
    public static long[] getAllocatedBytes(long[] threadIds) {
        if (threadsAllocatedBytes == null) {
            throw new UnsupportedOperationException("Allocated bytes are not supported");
        }

        try {
            return (long[]) threadsAllocatedBytes.invokeExact(threadIds);
        } catch (Throwable t) {
            throw throwUnchecked0(t); // Should never occur, but if it does it's guaranteed to be a runtime exception.
        }
    }

    @SuppressWarnings({ "unchecked", "TypeParameterUnusedInFormals" })
    private static <T extends Throwable> T throwUnchecked0(Throwable t) throws T {
        throw (T) t;
    }
}
