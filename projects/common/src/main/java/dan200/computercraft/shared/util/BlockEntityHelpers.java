/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.util;

import dan200.computercraft.shared.platform.PlatformHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

public final class BlockEntityHelpers {
    /**
     * The maximum limit a player can be away from a block to still have its UI open.
     *
     * @see #isUsable(BlockEntity, Player, double)
     */
    public static final double DEFAULT_INTERACT_RANGE = 8.0;

    private BlockEntityHelpers() {
    }

    @Nullable
    @SuppressWarnings("unchecked")
    public static <E extends BlockEntity, A extends BlockEntity> BlockEntityTicker<A> createTickerHelper(
        BlockEntityType<A> actualType, BlockEntityType<E> expectedType, BlockEntityTicker<? super E> ticker
    ) {
        return actualType == expectedType ? (BlockEntityTicker<A>) ticker : null;
    }

    /**
     * Determine if a block entity is "usable" by a player.
     *
     * @param blockEntity The current block entity.
     * @param player      The player who is trying to interact with the block.
     * @param range       The default distance the player can be away. This typically defaults to {@link #DEFAULT_INTERACT_RANGE},
     *                    but a custom value may be used. If {@link PlatformHelper#getReachDistance(Player)} is larger,
     *                    that will be used instead.
     * @return Whether this block entity is usable.
     */
    public static boolean isUsable(BlockEntity blockEntity, Player player, double range) {
        var level = blockEntity.getLevel();
        var pos = blockEntity.getBlockPos();

        range = Math.max(range, PlatformHelper.get().getReachDistance(player));

        return player.isAlive() && player.getCommandSenderWorld() == level &&
            !blockEntity.isRemoved() && level.getBlockEntity(pos) == blockEntity &&
            player.distanceToSqr(Vec3.atCenterOf(pos)) <= range * range;
    }

    /**
     * Update a block entity, marking it as changed and propagating changes to the client.
     *
     * @param blockEntity The block entity which has updated.
     */
    public static void updateBlock(BlockEntity blockEntity) {
        blockEntity.setChanged();

        var state = blockEntity.getBlockState();
        blockEntity.getLevel().sendBlockUpdated(blockEntity.getBlockPos(), state, state, Block.UPDATE_ALL);
    }
}
