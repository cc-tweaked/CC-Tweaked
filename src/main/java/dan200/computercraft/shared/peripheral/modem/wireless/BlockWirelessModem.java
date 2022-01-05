/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.peripheral.modem.wireless;

import dan200.computercraft.shared.common.BlockGeneric;
import dan200.computercraft.shared.computer.core.ComputerFamily;
import dan200.computercraft.shared.peripheral.modem.ModemShapes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.function.Supplier;

import static dan200.computercraft.shared.util.WaterloggableHelpers.*;

public class BlockWirelessModem extends BlockGeneric implements SimpleWaterloggedBlock
{
    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    public static final BooleanProperty ON = BooleanProperty.create( "on" );

    private final ComputerFamily family;

    public BlockWirelessModem( Properties settings, Supplier<BlockEntityType<? extends TileWirelessModem>> type, ComputerFamily family )
    {
        super( settings, type );
        this.family = family;
        registerDefaultState( getStateDefinition().any()
            .setValue( FACING, Direction.NORTH )
            .setValue( ON, false )
            .setValue( WATERLOGGED, false ) );
    }

    @Nonnull
    @Override
    @Deprecated
    public BlockState updateShape( @Nonnull BlockState state, @Nonnull Direction side, @Nonnull BlockState otherState,
                                   @Nonnull LevelAccessor world, @Nonnull BlockPos pos, @Nonnull BlockPos otherPos )
    {
        updateWaterloggedPostPlacement( state, world, pos );
        return side == state.getValue( FACING ) && !state.canSurvive( world, pos ) ? state.getFluidState()
            .createLegacyBlock() : state;
    }

    @Nonnull
    @Override
    @Deprecated
    public FluidState getFluidState( @Nonnull BlockState state )
    {
        return getWaterloggedFluidState( state );
    }

    @Override
    @Deprecated
    public boolean canSurvive( BlockState state, @Nonnull LevelReader world, BlockPos pos )
    {
        Direction facing = state.getValue( FACING );
        return canSupportCenter( world, pos.relative( facing ), facing.getOpposite() );
    }

    @Nonnull
    @Override
    @Deprecated
    public VoxelShape getShape( BlockState blockState, @Nonnull BlockGetter blockView, @Nonnull BlockPos blockPos, @Nonnull CollisionContext context )
    {
        return ModemShapes.getBounds( blockState.getValue( FACING ) );
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement( BlockPlaceContext placement )
    {
        return defaultBlockState().setValue( FACING,
                placement.getClickedFace()
                    .getOpposite() )
            .setValue( WATERLOGGED, getWaterloggedStateForPlacement( placement ) );
    }

    @Override
    protected void createBlockStateDefinition( StateDefinition.Builder<Block, BlockState> builder )
    {
        builder.add( FACING, ON, WATERLOGGED );
    }
}
