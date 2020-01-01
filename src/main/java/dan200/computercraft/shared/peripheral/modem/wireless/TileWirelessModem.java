/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.peripheral.modem.wireless;

import dan200.computercraft.shared.common.IDirectionalTile;
import dan200.computercraft.shared.peripheral.PeripheralType;
import dan200.computercraft.shared.peripheral.common.BlockPeripheral;
import dan200.computercraft.shared.peripheral.common.BlockPeripheralVariant;
import dan200.computercraft.shared.peripheral.common.ITilePeripheral;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nonnull;

public class TileWirelessModem extends TileWirelessModemBase implements IDirectionalTile, ITilePeripheral
{
    public TileWirelessModem()
    {
        super( false );
    }

    @Override
    public EnumFacing getDirection()
    {
        // Wireless Modem
        IBlockState state = getBlockState();
        switch( state.getValue( BlockPeripheral.VARIANT ) )
        {
            case WirelessModemDownOff:
            case WirelessModemDownOn:
                return EnumFacing.DOWN;
            case WirelessModemUpOff:
            case WirelessModemUpOn:
                return EnumFacing.UP;
            default:
                return state.getValue( BlockPeripheral.FACING );
        }
    }

    @Override
    public void setDirection( EnumFacing dir )
    {
        // Wireless Modem
        if( dir == EnumFacing.UP )
        {
            setBlockState( getBlockState()
                .withProperty( BlockPeripheral.VARIANT, BlockPeripheralVariant.WirelessModemUpOff )
                .withProperty( BlockPeripheral.FACING, EnumFacing.NORTH )
            );
        }
        else if( dir == EnumFacing.DOWN )
        {
            setBlockState( getBlockState()
                .withProperty( BlockPeripheral.VARIANT, BlockPeripheralVariant.WirelessModemDownOff )
                .withProperty( BlockPeripheral.FACING, EnumFacing.NORTH )
            );
        }
        else
        {
            setBlockState( getBlockState()
                .withProperty( BlockPeripheral.VARIANT, BlockPeripheralVariant.WirelessModemOff )
                .withProperty( BlockPeripheral.FACING, dir )
            );
        }
    }

    @Override
    public boolean shouldRefresh( World world, BlockPos pos, @Nonnull IBlockState oldState, @Nonnull IBlockState newState )
    {
        return super.shouldRefresh( world, pos, oldState, newState ) || BlockPeripheral.getPeripheralType( newState ) != PeripheralType.WirelessModem;
    }

    @Override
    public PeripheralType getPeripheralType()
    {
        return PeripheralType.WirelessModem;
    }
}
