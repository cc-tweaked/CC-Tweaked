/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.computer.blocks;

import dan200.computercraft.shared.computer.core.ComputerFamily;
import dan200.computercraft.shared.computer.core.ComputerState;
import dan200.computercraft.shared.computer.items.ComputerItemFactory;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.function.Supplier;

public class BlockComputer extends BlockComputerBase<TileComputer>
{
    public static final EnumProperty<ComputerState> STATE = EnumProperty.create( "state", ComputerState.class );
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    public BlockComputer( Properties settings, ComputerFamily family, Supplier<BlockEntityType<? extends TileComputer>> type )
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
}
