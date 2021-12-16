/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.computer.blocks;

import dan200.computercraft.shared.ComputerCraftRegistry;
import dan200.computercraft.shared.computer.core.ComputerFamily;
import dan200.computercraft.shared.computer.core.ComputerState;
import dan200.computercraft.shared.computer.items.ComputerItemFactory;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class BlockComputer extends BlockComputerBase<TileComputer>
{
    public static final EnumProperty<ComputerState> STATE = EnumProperty.create( "state", ComputerState.class );
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    public BlockComputer( Properties settings, ComputerFamily family, BlockEntityType<? extends TileComputer> type )
    {
        super( settings, family, type );
        registerDefaultState( defaultBlockState().setValue( FACING, Direction.NORTH )
            .setValue( STATE, ComputerState.OFF ) );
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement( BlockPlaceContext placement )
    {
        return defaultBlockState().setValue( FACING,
            placement.getHorizontalDirection()
                .getOpposite() );
    }

    @Override
    protected void createBlockStateDefinition( StateDefinition.Builder<Block, BlockState> builder )
    {
        builder.add( FACING, STATE );
    }

    @Nonnull
    @Override
    protected ItemStack getItem( TileComputerBase tile )
    {
        return tile instanceof TileComputer ? ComputerItemFactory.create( (TileComputer) tile ) : ItemStack.EMPTY;
    }

    public BlockEntityType<? extends TileComputer> getTypeByFamily( ComputerFamily family )
    {
        return switch( family )
        {
            case COMMAND -> ComputerCraftRegistry.ModTiles.COMPUTER_COMMAND;
            case ADVANCED -> ComputerCraftRegistry.ModTiles.COMPUTER_ADVANCED;
            default -> ComputerCraftRegistry.ModTiles.COMPUTER_NORMAL;
        };
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity( BlockPos pos, BlockState state )
    {
        return new TileComputer( getFamily(), getTypeByFamily( getFamily() ), pos, state );
    }
}
