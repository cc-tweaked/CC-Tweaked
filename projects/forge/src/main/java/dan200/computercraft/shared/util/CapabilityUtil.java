// SPDX-FileCopyrightText: 2020 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.BlockCapability;
import org.jetbrains.annotations.Nullable;

public final class CapabilityUtil {
    private CapabilityUtil() {
    }

    /**
     * Find a capability, preferring the internal/null side but falling back to a given side if a mod doesn't support
     * the internal one.
     *
     * @param level       The server level.
     * @param capability  The capability to get.
     * @param pos         The block position.
     * @param state       The block state.
     * @param blockEntity The block entity.
     * @param side        The side we'll fall back to.
     * @param <T>         The type of the underlying capability.
     * @return The extracted capability, if present.
     */
    public static <T> @Nullable T getCapability(ServerLevel level, BlockCapability<T, @Nullable Direction> capability, BlockPos pos, BlockState state, @Nullable BlockEntity blockEntity, @Nullable Direction side) {
        var cap = level.getCapability(capability, pos, state, blockEntity, null);
        return cap == null && side != null ? level.getCapability(capability, pos, state, blockEntity, side) : cap;
    }
}
