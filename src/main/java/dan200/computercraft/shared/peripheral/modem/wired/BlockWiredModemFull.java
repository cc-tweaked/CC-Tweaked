/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.peripheral.modem.wired;

import dan200.computercraft.shared.common.BlockGeneric;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.state.BooleanProperty;
import net.minecraft.state.StateContainer;

public class BlockWiredModemFull extends BlockGeneric
{
    public static final BooleanProperty MODEM_ON = BooleanProperty.create( "modem" );
    public static final BooleanProperty PERIPHERAL_ON = BooleanProperty.create( "peripheral" );

    public BlockWiredModemFull( Properties settings )
    {
        super( settings, TileWiredModemFull.FACTORY );
        setDefaultState( getStateContainer().getBaseState()
            .with( MODEM_ON, false )
            .with( PERIPHERAL_ON, false )
        );
    }

    @Override
    protected void fillStateContainer( StateContainer.Builder<Block, IBlockState> builder )
    {
        builder.add( MODEM_ON, PERIPHERAL_ON );
    }
}
