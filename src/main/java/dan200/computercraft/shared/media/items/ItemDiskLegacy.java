/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.media.items;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.api.filesystem.IMount;
import dan200.computercraft.api.media.IMedia;
import dan200.computercraft.shared.common.IColouredItem;
import dan200.computercraft.shared.util.Colour;
import dan200.computercraft.shared.util.StringUtil;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import java.util.List;

public class ItemDiskLegacy extends Item implements IMedia, IColouredItem
{
    public ItemDiskLegacy()
    {
        setMaxStackSize( 1 );
        setHasSubtypes( true );
        setTranslationKey( "computercraft:disk" );
        setCreativeTab( ComputerCraft.mainCreativeTab );
    }

    @Override
    public void getSubItems( @Nonnull CreativeTabs tabs, @Nonnull NonNullList<ItemStack> list )
    {
        if( !isInCreativeTab( tabs ) ) return;
        for( int colour = 0; colour < 16; colour++ )
        {
            ItemStack stack = createFromIDAndColour( -1, null, Colour.values()[colour].getHex() );
            if( stack.getItem() == this )
            {
                list.add( stack );
            }
        }
    }

    @Nonnull
    public static ItemStack createFromIDAndColour( int id, String label, int colour )
    {
        return ItemDiskExpanded.createFromIDAndColour( id, label, colour );
    }

    public int getDiskID( @Nonnull ItemStack stack )
    {
        int damage = stack.getItemDamage();
        if( damage > 0 )
        {
            return damage;
        }
        return -1;
    }

    protected void setDiskID( @Nonnull ItemStack stack, int id )
    {
        if( id > 0 )
        {
            stack.setItemDamage( id );
        }
        else
        {
            stack.setItemDamage( 0 );
        }
    }

    @Override
    public void addInformation( @Nonnull ItemStack stack, World world, List<String> list, ITooltipFlag flag )
    {
        if( flag.isAdvanced() || getLabel( stack ) == null )
        {
            int id = getDiskID( stack );
            if( id >= 0 ) list.add( StringUtil.translateFormatted( "gui.computercraft.tooltip.disk_id", id ) );
        }
    }

    // IMedia implementation

    @Override
    public String getLabel( @Nonnull ItemStack stack )
    {
        if( stack.hasDisplayName() )
        {
            return stack.getDisplayName();
        }
        return null;
    }

    @Override
    public boolean setLabel( @Nonnull ItemStack stack, String label )
    {
        if( label != null )
        {
            stack.setStackDisplayName( label );
        }
        else
        {
            stack.clearCustomName();
        }
        return true;
    }

    @Override
    public IMount createDataMount( @Nonnull ItemStack stack, @Nonnull World world )
    {
        int diskID = getDiskID( stack );
        if( diskID < 0 )
        {
            diskID = ComputerCraftAPI.createUniqueNumberedSaveDir( world, "computer/disk" );
            setDiskID( stack, diskID );
        }
        return ComputerCraftAPI.createSaveDirMount( world, "computer/disk/" + diskID, ComputerCraft.floppySpaceLimit );
    }

    @Override
    public int getColour( @Nonnull ItemStack stack )
    {
        return Colour.Blue.getHex();
    }

    @Override
    public boolean doesSneakBypassUse( @Nonnull ItemStack stack, IBlockAccess world, BlockPos pos, EntityPlayer player )
    {
        return true;
    }

    @Override
    public ItemStack withColour( ItemStack stack, int colour )
    {
        return ItemDiskExpanded.createFromIDAndColour( getDiskID( stack ), getLabel( stack ), colour );
    }
}
