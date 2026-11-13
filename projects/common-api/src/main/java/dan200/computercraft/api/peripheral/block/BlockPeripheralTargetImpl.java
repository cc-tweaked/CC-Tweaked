// SPDX-FileCopyrightText: 2026 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.api.peripheral.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

record BlockPeripheralTargetImpl(
    ServerLevel level,
    BlockPos pos,
    @Nullable BlockEntity blockEntity,
    @Nullable Direction direction
) implements BlockPeripheralTarget {
    @Override
    public ServerLevel getLevel() {
        return level();
    }

    @Override
    public BlockPos getPos() {
        return pos();
    }

    @Override
    public BlockState getBlockState() {
        return blockEntity == null || blockEntity.isRemoved() ? getLevel().getBlockState(pos) : blockEntity.getBlockState();
    }

    @Override
    public @Nullable BlockEntity getBlockEntity() {
        return blockEntity();
    }

    @Override
    public @Nullable Direction getSide() {
        return direction();
    }
}
