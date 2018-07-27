/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2017. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.media.items;

import dan200.computercraft.ComputerCraft;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.NonNullList;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import java.util.List;

public class ItemPrintout extends Item
{
    public static final int LINES_PER_PAGE = 21;
    public static final int LINE_MAX_LENGTH = 25;
    public static final int MAX_PAGES = 16;

    public ItemPrintout()
    {
        setMaxStackSize( 1 );
        setCreativeTab( ComputerCraft.mainCreativeTab );
    }

    @Nonnull
    @Override
    public String getTranslationKey()
    {
        return "item." + getRegistryName();
    }

    @Nonnull
    @Override
    public String getTranslationKey( ItemStack stack )
    {
        return getTranslationKey();
    }

    @Override
    public void getSubItems( @Nonnull CreativeTabs tabs, @Nonnull NonNullList<ItemStack> list )
    {
        if( !isInCreativeTab( tabs ) ) return;
        list.add( createFromTitleAndText( null, new String[LINES_PER_PAGE], new String[LINES_PER_PAGE] ) );
    }

    @Override
    public void addInformation( @Nonnull ItemStack itemstack, World world, List<String> list, ITooltipFlag flag )
    {
        String title = getTitle( itemstack );
        if( title != null && title.length() > 0 )
        {
            list.add( title );
        }
    }

    @Nonnull
    @Override
    public ActionResult<ItemStack> onItemRightClick( World world, EntityPlayer player, @Nonnull EnumHand hand )
    {
        if( !world.isRemote ) ComputerCraft.openPrintoutGUI( player, hand );
        return new ActionResult<>( EnumActionResult.SUCCESS, player.getHeldItem( hand ) );
    }

    @Nonnull
    private ItemStack createFromTitleAndText( String title, String[] text, String[] colours )
    {
        // Create stack
        ItemStack stack = new ItemStack( this );

        // Build NBT
        NBTTagCompound nbt = new NBTTagCompound();
        if( title != null ) nbt.setString( "title", title );

        if( text != null )
        {
            nbt.setInteger( "pages", text.length / LINES_PER_PAGE );
            for( int i = 0; i < text.length; ++i )
            {
                if( text[i] != null ) nbt.setString( "line" + i, text[i] );
            }
        }
        if( colours != null )
        {
            for( int i = 0; i < colours.length; ++i )
            {
                if( colours[i] != null ) nbt.setString( "colour" + i, colours[i] );
            }
        }
        stack.setTagCompound( nbt );

        // Return stack
        return stack;
    }

    @Nonnull
    public static ItemStack createSingleFromTitleAndText( String title, String[] text, String[] colours )
    {
        return ComputerCraft.Items.printedPage.createFromTitleAndText( title, text, colours );
    }

    @Nonnull
    public static ItemStack createMultipleFromTitleAndText( String title, String[] text, String[] colours )
    {
        return ComputerCraft.Items.printedPages.createFromTitleAndText( title, text, colours );
    }

    @Nonnull
    public static ItemStack createBookFromTitleAndText( String title, String[] text, String[] colours )
    {
        return ComputerCraft.Items.printedBook.createFromTitleAndText( title, text, colours );
    }

    public static String getTitle( @Nonnull ItemStack stack )
    {
        NBTTagCompound nbt = stack.getTagCompound();
        return nbt != null && nbt.hasKey( "title" ) ? nbt.getString( "title" ) : null;
    }

    public static int getPageCount( @Nonnull ItemStack stack )
    {
        NBTTagCompound nbt = stack.getTagCompound();
        return nbt != null && nbt.hasKey( "pages" ) ? nbt.getInteger( "pages" ) : 1;
    }

    public static String[] getText( @Nonnull ItemStack stack )
    {
        NBTTagCompound nbt = stack.getTagCompound();
        int numLines = getPageCount( stack ) * LINES_PER_PAGE;
        String[] lines = new String[numLines];
        for( int i = 0; i < lines.length; ++i )
        {
            lines[i] = nbt != null ? nbt.getString( "line" + i ) : "";
        }
        return lines;
    }

    public static String[] getColours( @Nonnull ItemStack stack )
    {
        NBTTagCompound nbt = stack.getTagCompound();
        int numLines = getPageCount( stack ) * LINES_PER_PAGE;
        String[] lines = new String[numLines];
        for( int i = 0; i < lines.length; ++i )
        {
            lines[i] = nbt != null ? nbt.getString( "colour" + i ) : "";
        }
        return lines;
    }
}
