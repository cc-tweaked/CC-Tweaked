// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.util;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;

import javax.annotation.Nullable;

public final class BlockEntityHelpers {
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
