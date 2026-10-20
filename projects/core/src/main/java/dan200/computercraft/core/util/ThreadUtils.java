// SPDX-FileCopyrightText: 2018 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.core.util;

import com.google.common.util.concurrent.CycleDetectingLockFactory;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Provides some utilities to create thread groups.
 */
public final class ThreadUtils {
    private static final Logger LOG = LoggerFactory.getLogger(ThreadUtils.class);
    private static final ThreadGroup baseGroup = new ThreadGroup("ComputerCraft");

    /**
     * A factory for locks. When in a development environment we enable cycle detection.
     */
    @SuppressWarnings("UnstableApiUsage")
    private static final CycleDetectingLockFactory lockFactory = CycleDetectingLockFactory.newInstance(
        // We don't have access to the mod-loader APIs here, so manually check for Fabric's development system property,
        // and guess based on what FML properties are set.
        ThreadUtils.class.desiredAssertionStatus() || isPropertySet("fabric.development") || isPropertySet("fml.modFolders")
            ? CycleDetectingLockFactory.Policies.THROW
            : CycleDetectingLockFactory.Policies.DISABLED
    );

    /**
     * A lower thread priority (though not the minimum), used for most of ComputerCraft's threads.
     * <p>
     * The Minecraft thread typically runs on a higher priority thread anyway, but this ensures we don't dominate other,
     * more critical work.
     *
     * @see Thread#setPriority(int)
     */
    public static final int LOWER_PRIORITY = (Thread.MIN_PRIORITY + Thread.NORM_PRIORITY) / 2;

    private ThreadUtils() {
    }

    private static boolean isPropertySet(String name) {
        var value = System.getProperty(name);
        return value != null && !name.equals("false");
    }

    /**
     * Get the base thread group, that all off-thread ComputerCraft activities are run on.
     *
     * @return The ComputerCraft group.
     */
    public static ThreadGroup group() {
        return baseGroup;
    }

    /**
     * Create a new {@link ThreadFactoryBuilder}, which constructs threads under a group of the given {@code name}.
     * <p>
     * Each thread will be of the format {@code ComputerCraft-<name>-<number>}, and belong to a group
     * called {@code ComputerCraft-<name>} (which in turn will be a child group of the main {@code ComputerCraft} group.
     *
     * @param name The name for the thread group and child threads.
     * @return The constructed thread factory builder, which may be extended with other properties.
     * @see #factory(String)
     */
    public static ThreadFactoryBuilder builder(String name) {
        var fullName = baseGroup.getName() + "-" + name;
        var group = new ThreadGroup(baseGroup, fullName);
        return new ThreadFactoryBuilder()
            .setDaemon(true)
            .setNameFormat(fullName.replace("%", "%%") + "-%d")
            .setUncaughtExceptionHandler((t, e) -> LOG.error("Exception in thread {}", t.getName(), e))
            .setThreadFactory(x -> new Thread(group, x));
    }

    /**
     * Create a new {@link ThreadFactory}, which constructs threads under a group of the given {@code name}.
     * <p>
     * Each thread will be of the format {@code ComputerCraft-<name>-<number>}, and belong to a group
     * called {@code ComputerCraft-<name>} (which in turn will be a child group of the main {@code ComputerCraft} group.
     *
     * @param name The name for the thread group and child threads.
     * @return The constructed thread factory.
     * @see #builder(String)
     */
    public static ThreadFactory factory(String name) {
        return builder(name).build();
    }

    /**
     * Create a new {@link ThreadFactory}, which constructs threads under a group of the given {@code name}. This is the
     * same as {@link #factory(String)}, but threads will be created with a {@linkplain #LOWER_PRIORITY lower priority}.
     *
     * @param name The name for the thread group and child threads.
     * @return The constructed thread factory.
     * @see #builder(String)
     */
    public static ThreadFactory lowPriorityFactory(String name) {
        return builder(name).setPriority(LOWER_PRIORITY).build();
    }

    /**
     * Create a new {@link ReentrantLock} with the given name. When running in a development environment we enable cycle
     * detection
     *
     * @param name A user-readable name of the lock. This is typically in the form {@code ClassName.field}.
     * @return The new lock instance.
     */
    @SuppressWarnings("UnstableApiUsage")
    public static ReentrantLock newReentrantLock(String name) {
        return lockFactory.newReentrantLock(name);
    }

    /**
     * Create a new {@link ReentrantReadWriteLock} with the given name. When running in a development environment we enable cycle
     * detection
     *
     * @param name A user-readable name of the lock. This is typically in the form {@code ClassName.field}.
     * @return The new lock instance.
     */
    @SuppressWarnings("UnstableApiUsage")
    public static ReentrantReadWriteLock newReentrantReadWriteLock(String name) {
        return lockFactory.newReentrantReadWriteLock(name);
    }
}
