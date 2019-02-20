/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.peripheral.printer;

import dan200.computercraft.shared.common.BlockGeneric;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.state.BooleanProperty;
import net.minecraft.state.DirectionProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.util.EnumFacing;

import javax.annotation.Nullable;

public class BlockPrinter extends BlockGeneric
{
    static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    static final BooleanProperty TOP = BooleanProperty.create( "top" );
    static final BooleanProperty BOTTOM = BooleanProperty.create( "bottom" );

    public BlockPrinter( Properties settings )
    {
        super( settings, TilePrinter.FACTORY );
        setDefaultState( getStateContainer().getBaseState()
            .with( FACING, EnumFacing.NORTH )
            .with( TOP, false )
            .with( BOTTOM, false ) );
    }

    @Override
    protected void fillStateContainer( StateContainer.Builder<Block, IBlockState> properties )
    {
        properties.add( FACING, TOP, BOTTOM );
    }

    @Nullable
    @Override
    public IBlockState getStateForPlacement( BlockItemUseContext placement )
    {
        return getDefaultState().with( FACING, placement.getPlacementHorizontalFacing().getOpposite() );
    }
}
