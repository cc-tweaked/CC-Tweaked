/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.peripheral.modem.wired;

import dan200.computercraft.shared.ComputerCraftRegistry;
import dan200.computercraft.shared.common.BlockGeneric;
import dan200.computercraft.shared.peripheral.speaker.TileSpeaker;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.util.math.BlockPos;

import javax.annotation.Nullable;

public class BlockWiredModemFull extends BlockGeneric
{
    public static final BooleanProperty MODEM_ON = BooleanProperty.of( "modem" );
    public static final BooleanProperty PERIPHERAL_ON = BooleanProperty.of( "peripheral" );

    public BlockWiredModemFull( Settings settings, BlockEntityType<? extends TileWiredModemFull> type )
    {
        super( settings, type );
        setDefaultState( getStateManager().getDefaultState()
            .with( MODEM_ON, false )
            .with( PERIPHERAL_ON, false ) );
    }

    @Override
    protected void appendProperties( StateManager.Builder<Block, BlockState> builder )
    {
        builder.add( MODEM_ON, PERIPHERAL_ON );
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state)
    {
        return new TileWiredModemFull(ComputerCraftRegistry.ModTiles.WIRED_MODEM_FULL, pos, state);
    }
}
