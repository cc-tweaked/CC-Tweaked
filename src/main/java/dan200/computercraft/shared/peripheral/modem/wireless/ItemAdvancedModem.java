/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.peripheral.modem.wireless;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.shared.peripheral.PeripheralType;
import dan200.computercraft.shared.peripheral.common.ItemPeripheralBase;
import dan200.computercraft.shared.peripheral.common.PeripheralItemFactory;
import net.minecraft.block.Block;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class ItemAdvancedModem extends ItemPeripheralBase
{
    public ItemAdvancedModem( Block block )
    {
        super( block );
        setTranslationKey( "computercraft:advanced_modem" );
        setCreativeTab( ComputerCraft.mainCreativeTab );
    }

    @Override
    public void getSubItems( @Nullable CreativeTabs tabs, @Nonnull NonNullList<ItemStack> list )
    {
        if( !isInCreativeTab( tabs ) ) return;
        list.add( PeripheralItemFactory.create( PeripheralType.AdvancedModem, null, 1 ) );
    }

    @Override
    public PeripheralType getPeripheralType( int damage )
    {
        return PeripheralType.AdvancedModem;
    }
}
