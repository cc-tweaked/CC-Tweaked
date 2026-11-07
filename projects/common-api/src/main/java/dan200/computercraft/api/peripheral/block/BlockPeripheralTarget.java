// SPDX-FileCopyrightText: 2026 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.api.peripheral.block;

import dan200.computercraft.api.peripheral.IPeripheral;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * The target for a peripheral provided by a block or block entity.
 *
 * @see PeripheralTargets#getBlockTarget(IPeripheral)
 * @see BlockPeripheral#getTarget()
 * @since 1.122.0
 */
public sealed interface BlockPeripheralTarget permits BlockPeripheralTargetImpl {
    /**
     * Create a new {@link BlockPeripheralTarget} from a level and position.
     *
     * @param level       The block's level.
     * @param pos         The block's position.
     * @param blockEntity The block's block entity.
     * @param direction   The side of the block the peripheral targets, if known.
     * @return The new {@link BlockPeripheralTarget} instance.
     */
    static BlockPeripheralTarget of(ServerLevel level, BlockPos pos, @Nullable BlockEntity blockEntity, @Nullable Direction direction) {
        if (blockEntity != null && (blockEntity.getLevel() != level || !blockEntity.getBlockPos().equals(pos))) {
            throw new IllegalArgumentException("BlockEntity is not at this level and position");
        }

        return new BlockPeripheralTargetImpl(level, pos, blockEntity, direction);
    }

    /**
     * Create a new {@link BlockPeripheralTarget} from a block entity.
     *
     * @param blockEntity The block entity this peripheral targets.
     * @param direction   The side of the block the peripheral targets, if known.
     * @return The new {@link BlockPeripheralTarget} instance.
     */
    static BlockPeripheralTarget of(BlockEntity blockEntity, @Nullable Direction direction) {
        var level = blockEntity.getLevel();
        if (level == null) throw new IllegalArgumentException("BlockEntity is not in level");
        return new BlockPeripheralTargetImpl((ServerLevel) level, blockEntity.getBlockPos(), blockEntity, direction);
    }

    /**
     * Get the level the block is in.
     *
     * @return The block's level.
     */
    ServerLevel getLevel();

    /**
     * Get the position of the block.
     *
     * @return The block's position.
     */
    BlockPos getPos();

    /**
     * Get the <em>current</em> {@link BlockState} of the block. This is fetched from the world when called, so may
     * change between calls.
     *
     * @return The current block state;
     */
    BlockState getBlockState();

    /**
     * Get the {@link BlockEntity} for this block.
     * <p>
     * Unlike {@link #getBlockState()}, peripherals do not typically change block entity, so this function will always
     * return the same value.
     *
     * @return The blocks' {@link BlockEntity}.
     */
    @Nullable BlockEntity getBlockEntity();

    /**
     * Get the side of the block this peripheral comes from. This may be {@code null} if the same peripheral is provided
     * on all sides.
     *
     * @return The side this peripheral comes from.
     */
    @Nullable Direction getSide();
}
