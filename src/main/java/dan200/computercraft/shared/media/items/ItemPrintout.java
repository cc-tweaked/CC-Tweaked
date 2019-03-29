/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.media.items;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.shared.network.Containers;
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
    private static final String NBT_TITLE = "title";
    private static final String NBT_PAGES = "pages";
    private static final String NBT_LINE_TEXT = "line";
    private static final String NBT_LINE_COLOUR = "colour";

    public static final int LINES_PER_PAGE = 21;
    public static final int LINE_MAX_LENGTH = 25;
    public static final int MAX_PAGES = 16;

    public enum Type
    {
        Single,
        Multiple,
        Book
    }

    public ItemPrintout()
    {
        setMaxStackSize( 1 );
        setHasSubtypes( true );
        setTranslationKey( "computercraft:page" );
        setCreativeTab( ComputerCraft.mainCreativeTab );
    }

    @Override
    public void getSubItems( @Nonnull CreativeTabs tabs, @Nonnull NonNullList<ItemStack> list )
    {
        if( !isInCreativeTab( tabs ) ) return;
        list.add( createSingleFromTitleAndText( null, new String[LINES_PER_PAGE], new String[LINES_PER_PAGE] ) );
        list.add( createMultipleFromTitleAndText( null, new String[2 * LINES_PER_PAGE], new String[2 * LINES_PER_PAGE] ) );
        list.add( createBookFromTitleAndText( null, new String[2 * LINES_PER_PAGE], new String[2 * LINES_PER_PAGE] ) );
    }

    @Override
    public void addInformation( @Nonnull ItemStack itemstack, World world, List<String> list, ITooltipFlag flag )
    {
        String title = getTitle( itemstack );
        if( title != null && !title.isEmpty() ) list.add( title );
    }

    @Nonnull
    @Override
    public String getTranslationKey( @Nonnull ItemStack stack )
    {
        Type type = getType( stack );
        switch( type )
        {
            case Single:
            default:
                return "item.computercraft:page";
            case Multiple:
                return "item.computercraft:pages";
            case Book:
                return "item.computercraft:book";
        }
    }

    @Nonnull
    @Override
    public ActionResult<ItemStack> onItemRightClick( World world, EntityPlayer player, @Nonnull EnumHand hand )
    {
        if( !world.isRemote ) Containers.openPrintoutGUI( player, hand );
        return new ActionResult<>( EnumActionResult.SUCCESS, player.getHeldItem( hand ) );
    }

    @Nonnull
    private static ItemStack createFromTitleAndText( Type type, String title, String[] text, String[] colours )
    {
        // Calculate damage
        int damage;
        switch( type )
        {
            case Single:
            default:
                damage = 0;
                break;
            case Multiple:
                damage = 1;
                break;
            case Book:
                damage = 2;
                break;
        }

        // Create stack
        ItemStack stack = new ItemStack( ComputerCraft.Items.printout, 1, damage );

        // Build NBT
        NBTTagCompound nbt = new NBTTagCompound();
        if( title != null ) nbt.setString( NBT_TITLE, title );
        if( text != null )
        {
            nbt.setInteger( NBT_PAGES, text.length / LINES_PER_PAGE );
            for( int i = 0; i < text.length; i++ )
            {
                if( text[i] != null ) nbt.setString( NBT_LINE_TEXT + i, text[i] );
            }
        }
        if( colours != null )
        {
            for( int i = 0; i < colours.length; i++ )
            {
                if( colours[i] != null ) nbt.setString( NBT_LINE_COLOUR + i, colours[i] );
            }
        }
        stack.setTagCompound( nbt );

        // Return stack
        return stack;
    }

    @Nonnull
    public static ItemStack createSingleFromTitleAndText( String title, String[] text, String[] colours )
    {
        return createFromTitleAndText( Type.Single, title, text, colours );
    }

    @Nonnull
    public static ItemStack createMultipleFromTitleAndText( String title, String[] text, String[] colours )
    {
        return createFromTitleAndText( Type.Multiple, title, text, colours );
    }

    @Nonnull
    public static ItemStack createBookFromTitleAndText( String title, String[] text, String[] colours )
    {
        return createFromTitleAndText( Type.Book, title, text, colours );
    }

    public static Type getType( @Nonnull ItemStack stack )
    {
        int damage = stack.getItemDamage();
        switch( damage )
        {
            case 0:
            default:
                return Type.Single;
            case 1:
                return Type.Multiple;
            case 2:
                return Type.Book;
        }
    }

    public static String getTitle( @Nonnull ItemStack stack )
    {
        NBTTagCompound nbt = stack.getTagCompound();
        return nbt != null && nbt.hasKey( NBT_TITLE ) ? nbt.getString( NBT_TITLE ) : null;
    }

    public static int getPageCount( @Nonnull ItemStack stack )
    {
        NBTTagCompound nbt = stack.getTagCompound();
        return nbt != null && nbt.hasKey( NBT_PAGES ) ? nbt.getInteger( NBT_PAGES ) : 1;
    }

    public static String[] getText( @Nonnull ItemStack stack )
    {
        return getLines( stack, NBT_LINE_TEXT );
    }

    public static String[] getColours( @Nonnull ItemStack stack )
    {
        return getLines( stack, NBT_LINE_COLOUR );
    }

    private static String[] getLines( @Nonnull ItemStack stack, String prefix )
    {
        NBTTagCompound nbt = stack.getTagCompound();
        int numLines = getPageCount( stack ) * LINES_PER_PAGE;
        String[] lines = new String[numLines];
        for( int i = 0; i < lines.length; i++ )
        {
            lines[i] = nbt != null ? nbt.getString( prefix + i ) : "";
        }
        return lines;
    }
}
