// Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
//
// SPDX-License-Identifier: LicenseRef-CCPL

package dan200.computercraft.impl;

import dan200.computercraft.api.redstone.BundledRedstoneProvider;
import dan200.computercraft.shared.common.DefaultBundledRedstoneProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Objects;

public final class BundledRedstone {
    private static final Logger LOG = LoggerFactory.getLogger(BundledRedstone.class);

    private static final ArrayList<BundledRedstoneProvider> providers = new ArrayList<>();

    private BundledRedstone() {
    }

    public static synchronized void register(BundledRedstoneProvider provider) {
        Objects.requireNonNull(provider, "provider cannot be null");
        if (!providers.contains(provider)) providers.add(provider);
    }

    public static int getDefaultOutput(Level world, BlockPos pos, Direction side) {
        return world.isInWorldBounds(pos) ? DefaultBundledRedstoneProvider.getDefaultBundledRedstoneOutput(world, pos, side) : -1;
    }

    private static int getUnmaskedOutput(Level world, BlockPos pos, Direction side) {
        if (!world.isInWorldBounds(pos)) return -1;

        // Try the providers in order:
        var combinedSignal = -1;
        for (var bundledRedstoneProvider : providers) {
            try {
                var signal = bundledRedstoneProvider.getBundledRedstoneOutput(world, pos, side);
                if (signal >= 0) {
                    combinedSignal = combinedSignal < 0 ? signal & 0xffff : combinedSignal | (signal & 0xffff);
                }
            } catch (Exception e) {
                LOG.error("Bundled redstone provider " + bundledRedstoneProvider + " errored.", e);
            }
        }

        return combinedSignal;
    }

    public static int getOutput(Level world, BlockPos pos, Direction side) {
        var signal = getUnmaskedOutput(world, pos, side);
        return signal >= 0 ? signal : 0;
    }
}
