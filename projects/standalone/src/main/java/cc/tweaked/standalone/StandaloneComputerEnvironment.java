// SPDX-FileCopyrightText: 2023 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package cc.tweaked.standalone;

import dan200.computercraft.api.filesystem.WritableMount;
import dan200.computercraft.core.computer.ComputerEnvironment;
import dan200.computercraft.core.filesystem.MemoryMount;
import dan200.computercraft.core.filesystem.WritableFileMount;
import dan200.computercraft.core.metrics.MetricsObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.nio.file.Path;

/**
 * The {@link ComputerEnvironment} for our standalone emulator.
 */
public class StandaloneComputerEnvironment implements ComputerEnvironment {
    private static final Logger LOG = LoggerFactory.getLogger(StandaloneComputerEnvironment.class);

    @Nullable
    private final Path root;

    public StandaloneComputerEnvironment(@Nullable Path root) {
        this.root = root;
    }

    @Override
    public int getDay() {
        return 0;
    }

    @Override
    public double getTimeOfDay() {
        return 0;
    }

    @Override
    public WritableMount createRootMount() {
        if (root == null) {
            LOG.info("Creating in-memory mount.");
            return new MemoryMount();
        } else {
            LOG.info("Creating mount at {}.", root);
            return new WritableFileMount(root.toFile(), 1_000_000L);
        }
    }

    @Override
    public MetricsObserver getMetrics() {
        return MetricsObserver.discard();
    }
}
