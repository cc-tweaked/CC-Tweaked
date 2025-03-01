// SPDX-FileCopyrightText: 2024 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0
package dan200.computercraft.shared.peripheral.redstone;

import com.mojang.serialization.MapCodec;
import dan200.computercraft.shared.common.IBundledRedstoneBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;


/**
 * The block for redstone relays. This mostly just forwards method calls to the {@linkplain RedstoneRelayBlockEntity
 * block entity}.
 */
public final class RedstoneRelayBlock extends HorizontalDirectionalBlock implements EntityBlock, IBundledRedstoneBlock {
    private static final MapCodec<RedstoneRelayBlock> CODEC = simpleCodec(RedstoneRelayBlock::new);

    public RedstoneRelayBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> properties) {
        properties.add(FACING);
    }

    @Override
    protected MapCodec<RedstoneRelayBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext placement) {
        return defaultBlockState().setValue(FACING, placement.getHorizontalDirection());
    }

    @Override
    @Deprecated
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        super.tick(state, level, pos, random);

        if (level.getBlockEntity(pos) instanceof RedstoneRelayBlockEntity relay) relay.update();
    }

    @Override
    @Deprecated
    public boolean isSignalSource(BlockState state) {
        return true;
    }

    @Override
    @Deprecated
    public int getDirectSignal(BlockState state, BlockGetter level, BlockPos pos, Direction incomingSide) {
        return level.getBlockEntity(pos) instanceof RedstoneRelayBlockEntity relay ? relay.getRedstoneOutput(incomingSide.getOpposite()) : 0;
    }

    @Override
    @Deprecated
    public int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return getDirectSignal(state, level, pos, direction);
    }

    @Override
    public int getBundledRedstoneOutput(Level level, BlockPos pos, Direction side) {
        return level.getBlockEntity(pos) instanceof RedstoneRelayBlockEntity relay ? relay.getBundledRedstoneOutput(side) : 0;
    }

    @Override
    @Deprecated
    public void neighborChanged(BlockState state, Level world, BlockPos pos, Block neighbourBlock, BlockPos neighbourPos, boolean isMoving) {
        if (world.getBlockEntity(pos) instanceof RedstoneRelayBlockEntity relay) relay.neighborChanged(neighbourPos);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new RedstoneRelayBlockEntity(pos, state);
    }
}
