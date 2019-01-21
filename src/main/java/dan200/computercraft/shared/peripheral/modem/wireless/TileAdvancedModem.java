/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.peripheral.modem.wireless;

import dan200.computercraft.ComputerCraft;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.NonNullList;

import javax.annotation.Nonnull;

public class TileAdvancedModem extends TileWirelessModemBase
{
    @Override
    protected EnumFacing getDirection()
    {
        return getBlockState().getValue( BlockAdvancedModem.Properties.FACING );
    }

    @Override
    public void getDroppedItems( @Nonnull NonNullList<ItemStack> drops, boolean creative )
    {
        if( !creative ) drops.add( new ItemStack( ComputerCraft.Items.advancedModem ) );
    }
}
