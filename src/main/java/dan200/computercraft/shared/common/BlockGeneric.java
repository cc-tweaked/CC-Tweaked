/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.common;

import dan200.computercraft.shared.platform.RegistryEntry;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import javax.annotation.Nullable;

public abstract class BlockGeneric extends BaseEntityBlock {
    private final RegistryEntry<? extends BlockEntityType<? extends TileGeneric>> type;

    public BlockGeneric(Properties settings, RegistryEntry<? extends BlockEntityType<? extends TileGeneric>> type) {
        super(settings);
        this.type = type;
    }

    @Override
    @Deprecated
    public final void onRemove(BlockState block, Level world, BlockPos pos, BlockState replace, boolean bool) {
        if (block.getBlock() == replace.getBlock()) return;

        var tile = world.getBlockEntity(pos);
        super.onRemove(block, world, pos, replace, bool);
        world.removeBlockEntity(pos);
        if (tile instanceof TileGeneric generic) generic.destroy();
    }

    @Override
    @Deprecated
    public final InteractionResult use(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        var tile = world.getBlockEntity(pos);
        return tile instanceof TileGeneric generic ? generic.onActivate(player, hand, hit) : InteractionResult.PASS;
    }

    @Override
    @Deprecated
    public final void neighborChanged(BlockState state, Level world, BlockPos pos, Block neighbourBlock, BlockPos neighbourPos, boolean isMoving) {
        var tile = world.getBlockEntity(pos);
        if (tile instanceof TileGeneric generic) generic.onNeighbourChange(neighbourPos);
    }

    @Override
    public final void onNeighborChange(BlockState state, LevelReader world, BlockPos pos, BlockPos neighbour) {
        var tile = world.getBlockEntity(pos);
        if (tile instanceof TileGeneric generic) generic.onNeighbourTileEntityChange(neighbour);
    }

    @Override
    @Deprecated
    public void tick(BlockState state, ServerLevel world, BlockPos pos, RandomSource rand) {
        var te = world.getBlockEntity(pos);
        if (te instanceof TileGeneric generic) generic.blockTick();
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return type.get().create(pos, state);
    }

    @Override
    @Deprecated
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }
}
