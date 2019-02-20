/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.peripheral.diskdrive;

import dan200.computercraft.shared.common.BlockGeneric;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.state.DirectionProperty;
import net.minecraft.state.EnumProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.util.EnumFacing;

import javax.annotation.Nullable;

public class BlockDiskDrive extends BlockGeneric
{
    static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    static final EnumProperty<DiskDriveState> STATE = EnumProperty.create( "state", DiskDriveState.class );

    public BlockDiskDrive( Properties settings )
    {
        super( settings, TileDiskDrive.FACTORY );
        setDefaultState( getStateContainer().getBaseState()
            .with( FACING, EnumFacing.NORTH )
            .with( STATE, DiskDriveState.EMPTY ) );
    }


    @Override
    protected void fillStateContainer( StateContainer.Builder<Block, IBlockState> properties )
    {
        properties.add( FACING, STATE );
    }

    @Nullable
    @Override
    public IBlockState getStateForPlacement( BlockItemUseContext placement )
    {
        return getDefaultState().with( FACING, placement.getPlacementHorizontalFacing().getOpposite() );
    }
}
