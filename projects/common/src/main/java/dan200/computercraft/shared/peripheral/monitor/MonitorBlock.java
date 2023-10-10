// Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
//
// SPDX-License-Identifier: LicenseRef-CCPL

package dan200.computercraft.shared.peripheral.monitor;

import dan200.computercraft.shared.platform.PlatformHelper;
import dan200.computercraft.shared.platform.RegistryEntry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;

import javax.annotation.Nullable;

public class MonitorBlock extends HorizontalDirectionalBlock implements EntityBlock {
    public static final DirectionProperty ORIENTATION = DirectionProperty.create("orientation",
        Direction.UP, Direction.DOWN, Direction.NORTH);

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final EnumProperty<MonitorEdgeState> STATE = EnumProperty.create("state", MonitorEdgeState.class);

    private final RegistryEntry<? extends BlockEntityType<? extends MonitorBlockEntity>> type;

    public MonitorBlock(Properties settings, RegistryEntry<? extends BlockEntityType<? extends MonitorBlockEntity>> type) {
        super(settings);
        this.type = type;

        // TODO: Test underwater - do we need isSolid at all?
        registerDefaultState(getStateDefinition().any()
            .setValue(ORIENTATION, Direction.NORTH)
            .setValue(FACING, Direction.NORTH)
            .setValue(STATE, MonitorEdgeState.NONE));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ORIENTATION, FACING, STATE);
    }

    @Override
    @Nullable
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        var pitch = context.getPlayer() == null ? 0 : context.getPlayer().getXRot();
        Direction orientation;
        if (pitch > 66.5f) {
            // If the player is looking down, place it facing upwards
            orientation = Direction.UP;
        } else if (pitch < -66.5f) {
            // If they're looking up, place it down.
            orientation = Direction.DOWN;
        } else {
            orientation = Direction.NORTH;
        }

        return defaultBlockState()
            .setValue(FACING, context.getHorizontalDirection().getOpposite())
            .setValue(ORIENTATION, orientation);
    }

    @Override
    @Deprecated
    public final void onRemove(BlockState block, Level world, BlockPos pos, BlockState replace, boolean bool) {
        if (block.getBlock() == replace.getBlock()) return;

        var tile = world.getBlockEntity(pos);
        super.onRemove(block, world, pos, replace, bool);
        if (tile instanceof MonitorBlockEntity generic) generic.destroy();
    }

    @Override
    @Deprecated
    public void tick(BlockState state, ServerLevel world, BlockPos pos, RandomSource rand) {
        var te = world.getBlockEntity(pos);
        if (te instanceof MonitorBlockEntity monitor) monitor.blockTick();
    }

    @Override
    @Deprecated
    public final InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (player.isCrouching() || !(level.getBlockEntity(pos) instanceof MonitorBlockEntity monitor) || monitor.getFront() != hit.getDirection()) {
            return InteractionResult.PASS;
        }

        if (!level.isClientSide) {
            monitor.monitorTouched(
                (float) (hit.getLocation().x - hit.getBlockPos().getX()),
                (float) (hit.getLocation().y - hit.getBlockPos().getY()),
                (float) (hit.getLocation().z - hit.getBlockPos().getZ())
            );
        }

        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void setPlacedBy(Level world, BlockPos pos, BlockState blockState, @Nullable LivingEntity livingEntity, ItemStack itemStack) {
        super.setPlacedBy(world, pos, blockState, livingEntity, itemStack);

        var entity = world.getBlockEntity(pos);
        if (entity instanceof MonitorBlockEntity monitor && !world.isClientSide) {
            // Defer the block update if we're being placed by another TE. See #691
            if (livingEntity == null || (livingEntity instanceof ServerPlayer player && PlatformHelper.get().isFakePlayer(player))) {
                monitor.updateNeighborsDeferred();
                return;
            }

            monitor.expand();
        }
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        return type.get().create(blockPos, blockState);
    }
}
