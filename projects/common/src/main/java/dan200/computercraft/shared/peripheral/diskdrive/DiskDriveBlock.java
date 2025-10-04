// Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
//
// SPDX-License-Identifier: LicenseRef-CCPL

package dan200.computercraft.shared.peripheral.diskdrive;

import com.mojang.serialization.MapCodec;
import dan200.computercraft.shared.ModRegistry;
import dan200.computercraft.shared.common.HorizontalContainerBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import org.jspecify.annotations.Nullable;

public class DiskDriveBlock extends HorizontalContainerBlock {
    private static final MapCodec<DiskDriveBlock> CODEC = simpleCodec(DiskDriveBlock::new);

    public static final EnumProperty<DiskDriveState> STATE = EnumProperty.create("state", DiskDriveState.class);

    private static final BlockEntityTicker<DiskDriveBlockEntity> serverTicker = (level, pos, state, drive) -> drive.serverTick();

    public DiskDriveBlock(Properties settings) {
        super(settings);
        registerDefaultState(getStateDefinition().any()
            .setValue(FACING, Direction.NORTH)
            .setValue(STATE, DiskDriveState.EMPTY));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> properties) {
        properties.add(FACING, STATE);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    /**
     * A default implementation of {@link Item#useOn(UseOnContext)} for items that can be placed into a drive.
     *
     * @param context The context of this item usage action.
     * @return Whether the item was placed or not.
     */
    public static InteractionResult defaultUseItemOn(UseOnContext context) {
        var level = context.getLevel();
        var blockPos = context.getClickedPos();
        var blockState = level.getBlockState(blockPos);
        if (blockState.is(ModRegistry.Blocks.DISK_DRIVE.get()) && blockState.getValue(STATE) == DiskDriveState.EMPTY) {
            if (!level.isClientSide && level.getBlockEntity(blockPos) instanceof DiskDriveBlockEntity drive && drive.getDiskStack().isEmpty()) {
                drive.setDiskStack(context.getItemInHand().split(1));
            }
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.PASS;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return ModRegistry.BlockEntities.DISK_DRIVE.get().create(pos, state);
    }

    @Override
    @Nullable
    public <U extends BlockEntity> BlockEntityTicker<U> getTicker(Level level, BlockState state, BlockEntityType<U> type) {
        return level.isClientSide ? null : BaseEntityBlock.createTickerHelper(type, ModRegistry.BlockEntities.DISK_DRIVE.get(), serverTicker);
    }
}
