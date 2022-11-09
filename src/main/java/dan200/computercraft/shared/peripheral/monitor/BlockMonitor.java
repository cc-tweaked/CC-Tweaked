/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.peripheral.monitor;

import dan200.computercraft.shared.common.BlockGeneric;
import dan200.computercraft.shared.platform.PlatformHelper;
import dan200.computercraft.shared.platform.RegistryEntry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;

import javax.annotation.Nullable;

public class BlockMonitor extends BlockGeneric {
    public static final DirectionProperty ORIENTATION = DirectionProperty.create("orientation",
        Direction.UP, Direction.DOWN, Direction.NORTH);

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    public static final EnumProperty<MonitorEdgeState> STATE = EnumProperty.create("state", MonitorEdgeState.class);

    public BlockMonitor(Properties settings, RegistryEntry<? extends BlockEntityType<? extends TileMonitor>> type) {
        super(settings, type);
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
    @Deprecated
    public BlockState mirror(BlockState state, Mirror mirrorIn) {
        return state.rotate(mirrorIn.getRotation(state.getValue(FACING)));
    }

    @Override
    @Deprecated
    public BlockState rotate(BlockState state, Rotation rot) {
        return state.setValue(FACING, rot.rotate(state.getValue(FACING)));
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
    public void setPlacedBy(Level world, BlockPos pos, BlockState blockState, @Nullable LivingEntity livingEntity, ItemStack itemStack) {
        super.setPlacedBy(world, pos, blockState, livingEntity, itemStack);

        var entity = world.getBlockEntity(pos);
        if (entity instanceof TileMonitor monitor && !world.isClientSide) {
            // Defer the block update if we're being placed by another TE. See #691
            if (livingEntity == null || (livingEntity instanceof ServerPlayer player && PlatformHelper.get().isFakePlayer(player))) {
                monitor.updateNeighborsDeferred();
                return;
            }

            monitor.expand();
        }
    }
}
