/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.peripheral.modem.wireless;

import dan200.computercraft.shared.common.GenericBlock;
import dan200.computercraft.shared.peripheral.modem.ModemShapes;
import dan200.computercraft.shared.platform.RegistryEntry;
import dan200.computercraft.shared.util.WaterloggableHelpers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;

import static dan200.computercraft.shared.util.WaterloggableHelpers.WATERLOGGED;
import static dan200.computercraft.shared.util.WaterloggableHelpers.getFluidStateForPlacement;

public class WirelessModemBlock extends GenericBlock implements SimpleWaterloggedBlock {
    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    public static final BooleanProperty ON = BooleanProperty.create("on");

    public WirelessModemBlock(Properties settings, RegistryEntry<? extends BlockEntityType<? extends WirelessModemBlockEntity>> type) {
        super(settings, type);
        registerDefaultState(getStateDefinition().any()
            .setValue(FACING, Direction.NORTH)
            .setValue(ON, false)
            .setValue(WATERLOGGED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, ON, WATERLOGGED);
    }

    @Override
    @Deprecated
    public VoxelShape getShape(BlockState blockState, BlockGetter blockView, BlockPos blockPos, CollisionContext context) {
        return ModemShapes.getBounds(blockState.getValue(FACING));
    }

    @Override
    @Deprecated
    public FluidState getFluidState(BlockState state) {
        return WaterloggableHelpers.getFluidState(state);
    }

    @Override
    @Deprecated
    public BlockState updateShape(BlockState state, Direction side, BlockState otherState, LevelAccessor world, BlockPos pos, BlockPos otherPos) {
        WaterloggableHelpers.updateShape(state, world, pos);
        return side == state.getValue(FACING) && !state.canSurvive(world, pos)
            ? state.getFluidState().createLegacyBlock()
            : state;
    }

    @Override
    @Deprecated
    public boolean canSurvive(BlockState state, LevelReader world, BlockPos pos) {
        var facing = state.getValue(FACING);
        return canSupportCenter(world, pos.relative(facing), facing.getOpposite());
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext placement) {
        return defaultBlockState()
            .setValue(FACING, placement.getClickedFace().getOpposite())
            .setValue(WATERLOGGED, getFluidStateForPlacement(placement));
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
}
