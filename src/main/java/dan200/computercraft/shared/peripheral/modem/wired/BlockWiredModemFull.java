/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.peripheral.modem.wired;

import dan200.computercraft.shared.Registry;
import dan200.computercraft.shared.common.BlockGeneric;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.state.BooleanProperty;
import net.minecraft.state.StateContainer;

public class BlockWiredModemFull extends BlockGeneric
{
    public static final BooleanProperty MODEM_ON = BooleanProperty.create( "modem" );
    public static final BooleanProperty PERIPHERAL_ON = BooleanProperty.create( "peripheral" );

    public BlockWiredModemFull( Properties settings )
    {
        super( settings, Registry.ModTiles.WIRED_MODEM_FULL );
        registerDefaultState( getStateDefinition().any()
            .setValue( MODEM_ON, false )
            .setValue( PERIPHERAL_ON, false )
        );
    }

    @Override
    protected void createBlockStateDefinition( StateContainer.Builder<Block, BlockState> builder )
    {
        builder.add( MODEM_ON, PERIPHERAL_ON );
    }
}
