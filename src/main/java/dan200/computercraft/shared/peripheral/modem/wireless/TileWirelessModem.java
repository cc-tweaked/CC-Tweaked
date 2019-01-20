/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.peripheral.modem.wireless;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.shared.common.IDirectionalTile;
import dan200.computercraft.shared.peripheral.PeripheralType;
import dan200.computercraft.shared.peripheral.common.BlockPeripheral;
import dan200.computercraft.shared.peripheral.common.BlockPeripheralVariant;
import dan200.computercraft.shared.peripheral.common.ITilePeripheral;
import dan200.computercraft.shared.peripheral.common.PeripheralItemFactory;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nonnull;

public class TileWirelessModem extends TileWirelessModemBase implements IDirectionalTile, ITilePeripheral
{
    @Override
    public EnumFacing getDirection()
    {
        // Wireless Modem
        IBlockState state = getBlockState();
        switch( state.getValue( BlockPeripheral.Properties.VARIANT ) )
        {
            case WirelessModemDownOff:
            case WirelessModemDownOn:
                return EnumFacing.DOWN;
            case WirelessModemUpOff:
            case WirelessModemUpOn:
                return EnumFacing.UP;
            default:
                return state.getValue( BlockPeripheral.Properties.FACING );
        }
    }

    @Override
    public void setDirection( EnumFacing dir )
    {
        // Wireless Modem
        if( dir == EnumFacing.UP )
        {
            setBlockState( getBlockState()
                .withProperty( BlockPeripheral.Properties.VARIANT, BlockPeripheralVariant.WirelessModemUpOff )
                .withProperty( BlockPeripheral.Properties.FACING, EnumFacing.NORTH )
            );
        }
        else if( dir == EnumFacing.DOWN )
        {
            setBlockState( getBlockState()
                .withProperty( BlockPeripheral.Properties.VARIANT, BlockPeripheralVariant.WirelessModemDownOff )
                .withProperty( BlockPeripheral.Properties.FACING, EnumFacing.NORTH )
            );
        }
        else
        {
            setBlockState( getBlockState()
                .withProperty( BlockPeripheral.Properties.VARIANT, BlockPeripheralVariant.WirelessModemOff )
                .withProperty( BlockPeripheral.Properties.FACING, dir )
            );
        }
    }

    @Override
    public boolean shouldRefresh( World world, BlockPos pos, @Nonnull IBlockState oldState, @Nonnull IBlockState newState )
    {
        return super.shouldRefresh( world, pos, oldState, newState ) || ComputerCraft.Blocks.peripheral.getPeripheralType( newState ) != PeripheralType.WirelessModem;
    }

    @Override
    public void getDroppedItems( @Nonnull NonNullList<ItemStack> drops, boolean creative )
    {
        if( !creative ) drops.add( PeripheralItemFactory.create( PeripheralType.WirelessModem, null, 1 ) );
    }

    @Override
    public PeripheralType getPeripheralType()
    {
        return PeripheralType.WirelessModem;
    }
}
