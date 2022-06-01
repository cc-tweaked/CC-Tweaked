/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.computer.blocks;

import dan200.computercraft.shared.computer.core.ComputerFamily;
import dan200.computercraft.shared.computer.core.ComputerState;
import dan200.computercraft.shared.computer.items.ComputerItemFactory;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.item.ItemStack;
import net.minecraft.state.DirectionProperty;
import net.minecraft.state.EnumProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.Direction;
import net.minecraft.util.Mirror;
import net.minecraft.util.Rotation;
import net.minecraftforge.fml.RegistryObject;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class BlockComputer extends BlockComputerBase<TileComputer>
{
    public static final EnumProperty<ComputerState> STATE = EnumProperty.create( "state", ComputerState.class );
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    public BlockComputer( Properties settings, ComputerFamily family, RegistryObject<? extends TileEntityType<? extends TileComputer>> type )
    {
        super( settings, family, type );
        registerDefaultState( defaultBlockState()
            .setValue( FACING, Direction.NORTH )
            .setValue( STATE, ComputerState.OFF )
        );
    }

    @Override
    protected void createBlockStateDefinition( StateContainer.Builder<Block, BlockState> builder )
    {
        builder.add( FACING, STATE );
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement( BlockItemUseContext placement )
    {
        return defaultBlockState().setValue( FACING, placement.getHorizontalDirection().getOpposite() );
    }

    @Nonnull
    @Override
    @Deprecated
    public BlockState mirror( BlockState state, Mirror mirrorIn )
    {
        return state.rotate( mirrorIn.getRotation( state.getValue( FACING ) ) );
    }

    @Nonnull
    @Override
    @Deprecated
    public BlockState rotate( BlockState state, Rotation rot )
    {
        return state.setValue( FACING, rot.rotate( state.getValue( FACING ) ) );
    }

    @Nonnull
    @Override
    protected ItemStack getItem( TileComputerBase tile )
    {
        return tile instanceof TileComputer ? ComputerItemFactory.create( (TileComputer) tile ) : ItemStack.EMPTY;
    }
}
