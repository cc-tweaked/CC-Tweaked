// SPDX-FileCopyrightText: 2023 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.peripheral.generic;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;

/**
 * Extract some component (for instance a capability on Forge, or a {@code BlockApiLookup} on Fabric) from a block and
 * block entity.
 *
 * @param <C> A platform-specific type, used for the invalidation callback.
 */
public interface ComponentLookup<C extends Runnable> {
    /**
     * Extract some component from a block in the world.
     *
     * @param level       The current level.
     * @param pos         The position of the block in the level.
     * @param state       The block state at that position.
     * @param blockEntity The block entity at that position.
     * @param side        The side of the block to extract the component from. Implementations should try to use a
     *                    sideless lookup first, but may fall back to a sided lookup if needed.
     * @param invalidate  An invalidation function to call if this component changes.
     * @return The found component, or {@code null} if not present.
     */
    @Nullable
    Object find(ServerLevel level, BlockPos pos, BlockState state, BlockEntity blockEntity, Direction side, C invalidate);
}
