/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.peripheral.speaker;

import dan200.computercraft.shared.Registry;
import dan200.computercraft.shared.common.BlockGeneric;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.state.DirectionProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.util.Direction;

import javax.annotation.Nullable;

public class BlockSpeaker extends BlockGeneric
{
    private static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    public BlockSpeaker( Properties settings )
    {
        super( settings, Registry.ModTiles.SPEAKER );
        registerDefaultState( getStateDefinition().any()
            .setValue( FACING, Direction.NORTH ) );
    }

    @Override
    protected void createBlockStateDefinition( StateContainer.Builder<Block, BlockState> properties )
    {
        properties.add( FACING );
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement( BlockItemUseContext placement )
    {
        return defaultBlockState().setValue( FACING, placement.getHorizontalDirection().getOpposite() );
    }
}
