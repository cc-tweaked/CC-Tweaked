// SPDX-FileCopyrightText: 2023 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.core.computer.mainthread;

import java.util.concurrent.TimeUnit;

/**
 * Options to configure the {@link MainThread}.
 */
public interface MainThreadConfig {
    /**
     * The default value for {@link #maxGlobalTime()}.
     */
    long DEFAULT_MAX_GLOBAL_TIME = TimeUnit.MILLISECONDS.toNanos(10);

    /**
     * The default value for {@link #maxComputerTime()}.
     */
    long DEFAULT_MAX_COMPUTER_TIME = TimeUnit.MILLISECONDS.toNanos(5);

    /**
     * The default config.
     */
    MainThreadConfig DEFAULT = new Basic(DEFAULT_MAX_GLOBAL_TIME, DEFAULT_MAX_COMPUTER_TIME);

    /**
     * The ideal maximum time that can be spent executing tasks in a tick, in nanoseconds.
     *
     * @return The max time a that can be spent executing tasks in a single tick.
     */
    long maxGlobalTime();

    /**
     * The ideal maximum time a computer can execute for in a tick, in nanoseconds.
     *
     * @return The max time a computer can execute in a single tick.
     */
    long maxComputerTime();

    /**
     * A basic implementation of {@link MainThreadConfig}, which works on constant values.
     *
     * @param maxGlobalTime   See {@link MainThreadConfig#maxGlobalTime()}.
     * @param maxComputerTime See {@link MainThreadConfig#maxComputerTime()}.
     */
    record Basic(long maxGlobalTime, long maxComputerTime) implements MainThreadConfig {
    }
}
