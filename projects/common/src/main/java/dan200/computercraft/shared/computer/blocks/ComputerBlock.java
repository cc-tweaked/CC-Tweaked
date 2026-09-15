// Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
//
// SPDX-License-Identifier: LicenseRef-CCPL

package dan200.computercraft.shared.computer.blocks;

import dan200.computercraft.shared.computer.core.ComputerState;
import dan200.computercraft.shared.platform.RegistryEntry;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import org.jspecify.annotations.Nullable;

public class ComputerBlock<T extends ComputerBlockEntity> extends AbstractComputerBlock<T> {
    public static final EnumProperty<ComputerState> STATE = EnumProperty.create("state", ComputerState.class);
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;

    public ComputerBlock(Properties settings, RegistryEntry<BlockEntityType<T>> type) {
        super(settings, type);
        registerDefaultState(defaultBlockState()
            .setValue(FACING, Direction.NORTH)
            .setValue(STATE, ComputerState.OFF)
        );
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, STATE);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext placement) {
        return defaultBlockState().setValue(FACING, placement.getHorizontalDirection().getOpposite());
    }
}
