/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.peripheral.modem.wireless;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.shared.integration.mcmp.MCMPHooks;
import dan200.computercraft.shared.peripheral.PeripheralType;
import dan200.computercraft.shared.peripheral.common.ItemPeripheralBase;
import dan200.computercraft.shared.peripheral.common.PeripheralItemFactory;
import net.minecraft.block.Block;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

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

    @Nonnull
    @Override
    public EnumActionResult onItemUse( EntityPlayer player, World worldIn, @Nonnull BlockPos pos, @Nonnull EnumHand hand, @Nonnull EnumFacing facing, float hitX, float hitY, float hitZ )
    {
        EnumActionResult result = MCMPHooks.onItemUse( this, player, worldIn, pos, hand, facing, hitX, hitY, hitZ );
        if( result != EnumActionResult.PASS ) return result;

        return super.onItemUse( player, worldIn, pos, hand, facing, hitX, hitY, hitZ );
    }
}
