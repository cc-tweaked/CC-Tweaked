/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.media.items;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.shared.network.Containers;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import java.util.List;

public class ItemPrintout extends Item
{
    private static final String NBT_TITLE = "Title";
    private static final String NBT_PAGES = "Pages";
    private static final String NBT_LINE_TEXT = "Text";
    private static final String NBT_LINE_COLOUR = "Color";

    public static final int LINES_PER_PAGE = 21;
    public static final int LINE_MAX_LENGTH = 25;
    public static final int MAX_PAGES = 16;

    public enum Type
    {
        PAGE,
        PAGES,
        BOOK
    }

    private final Type type;

    public ItemPrintout( Settings settings, Type type )
    {
        super( settings );
        this.type = type;
    }

    @Override
    public void appendTooltip( @Nonnull ItemStack stack, World world, List<Text> list, TooltipContext options )
    {
        String title = getTitle( stack );
        if( title != null && !title.isEmpty() ) list.add( new LiteralText( title ) );
    }

    @Override
    public TypedActionResult<ItemStack> use( World world, PlayerEntity player, Hand hand )
    {
        if( !world.isClient ) Containers.openPrintoutGUI( player, hand );
        return new TypedActionResult<>( ActionResult.SUCCESS, player.getStackInHand( hand ) );
    }

    @Nonnull
    private ItemStack createFromTitleAndText( String title, String[] text, String[] colours )
    {
        ItemStack stack = new ItemStack( this );

        // Build NBT
        if( title != null ) stack.getOrCreateTag().putString( NBT_TITLE, title );
        if( text != null )
        {
            CompoundTag tag = stack.getOrCreateTag();
            tag.putInt( NBT_PAGES, text.length / LINES_PER_PAGE );
            for( int i = 0; i < text.length; i++ )
            {
                if( text[i] != null ) tag.putString( NBT_LINE_TEXT + i, text[i] );
            }
        }
        if( colours != null )
        {
            CompoundTag tag = stack.getOrCreateTag();
            for( int i = 0; i < colours.length; i++ )
            {
                if( colours[i] != null ) tag.putString( NBT_LINE_COLOUR + i, colours[i] );
            }
        }


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

    public Type getType()
    {
        return type;
    }

    public static String getTitle( @Nonnull ItemStack stack )
    {
        CompoundTag nbt = stack.getTag();
        return nbt != null && nbt.containsKey( NBT_TITLE ) ? nbt.getString( NBT_TITLE ) : null;
    }

    public static int getPageCount( @Nonnull ItemStack stack )
    {
        CompoundTag nbt = stack.getTag();
        return nbt != null && nbt.containsKey( NBT_PAGES ) ? nbt.getInt( NBT_PAGES ) : 1;
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
        CompoundTag nbt = stack.getTag();
        int numLines = getPageCount( stack ) * LINES_PER_PAGE;
        String[] lines = new String[numLines];
        for( int i = 0; i < lines.length; i++ )
        {
            lines[i] = nbt != null ? nbt.getString( prefix + i ) : "";
        }
        return lines;
    }
}
