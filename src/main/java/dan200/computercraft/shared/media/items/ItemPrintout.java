/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.media.items;

import dan200.computercraft.shared.ComputerCraftRegistry;
import dan200.computercraft.shared.common.ContainerHeldItem;
import dan200.computercraft.shared.network.container.HeldItemContainerData;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
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
    public static final int LINES_PER_PAGE = 21;
    public static final int LINE_MAX_LENGTH = 25;
    public static final int MAX_PAGES = 16;
    private static final String NBT_TITLE = "Title";
    private static final String NBT_PAGES = "Pages";
    private static final String NBT_LINE_TEXT = "Text";
    private static final String NBT_LINE_COLOUR = "Color";
    private final Type type;

    public ItemPrintout( Settings settings, Type type )
    {
        super( settings );
        this.type = type;
    }

    @Nonnull
    public static ItemStack createSingleFromTitleAndText( String title, String[] text, String[] colours )
    {
        return ComputerCraftRegistry.ModItems.PRINTED_PAGE.createFromTitleAndText( title, text, colours );
    }

    @Nonnull
    private ItemStack createFromTitleAndText( String title, String[] text, String[] colours )
    {
        ItemStack stack = new ItemStack( this );

        // Build NBT
        if( title != null )
        {
            stack.getOrCreateTag()
                .putString( NBT_TITLE, title );
        }
        if( text != null )
        {
        	NbtCompound tag = stack.getOrCreateTag();
            tag.putInt( NBT_PAGES, text.length / LINES_PER_PAGE );
            for( int i = 0; i < text.length; i++ )
            {
                if( text[i] != null )
                {
                    tag.putString( NBT_LINE_TEXT + i, text[i] );
                }
            }
        }
        if( colours != null )
        {
        	NbtCompound tag = stack.getOrCreateTag();
            for( int i = 0; i < colours.length; i++ )
            {
                if( colours[i] != null )
                {
                    tag.putString( NBT_LINE_COLOUR + i, colours[i] );
                }
            }
        }


        return stack;
    }

    @Nonnull
    public static ItemStack createMultipleFromTitleAndText( String title, String[] text, String[] colours )
    {
        return ComputerCraftRegistry.ModItems.PRINTED_PAGES.createFromTitleAndText( title, text, colours );
    }

    @Nonnull
    public static ItemStack createBookFromTitleAndText( String title, String[] text, String[] colours )
    {
        return ComputerCraftRegistry.ModItems.PRINTED_BOOK.createFromTitleAndText( title, text, colours );
    }

    public static String[] getText( @Nonnull ItemStack stack )
    {
        return getLines( stack, NBT_LINE_TEXT );
    }

    private static String[] getLines( @Nonnull ItemStack stack, String prefix )
    {
    	NbtCompound nbt = stack.getTag();
        int numLines = getPageCount( stack ) * LINES_PER_PAGE;
        String[] lines = new String[numLines];
        for( int i = 0; i < lines.length; i++ )
        {
            lines[i] = nbt != null ? nbt.getString( prefix + i ) : "";
        }
        return lines;
    }

    public static int getPageCount( @Nonnull ItemStack stack )
    {
    	NbtCompound nbt = stack.getTag();
        return nbt != null && nbt.contains( NBT_PAGES ) ? nbt.getInt( NBT_PAGES ) : 1;
    }

    public static String[] getColours( @Nonnull ItemStack stack )
    {
        return getLines( stack, NBT_LINE_COLOUR );
    }

    @Nonnull
    @Override
    public TypedActionResult<ItemStack> use( World world, @Nonnull PlayerEntity player, @Nonnull Hand hand )
    {
        if( !world.isClient )
        {
            new HeldItemContainerData( hand ).open( player,
                new ContainerHeldItem.Factory( ComputerCraftRegistry.ModContainers.PRINTOUT,
                    player.getStackInHand( hand ),
                    hand ) );
        }
        return new TypedActionResult<>( ActionResult.SUCCESS, player.getStackInHand( hand ) );
    }

    @Override
    public void appendTooltip( @Nonnull ItemStack stack, World world, @Nonnull List<Text> list, @Nonnull TooltipContext options )
    {
        String title = getTitle( stack );
        if( title != null && !title.isEmpty() )
        {
            list.add( new LiteralText( title ) );
        }
    }

    public static String getTitle( @Nonnull ItemStack stack )
    {
    	NbtCompound nbt = stack.getTag();
        return nbt != null && nbt.contains( NBT_TITLE ) ? nbt.getString( NBT_TITLE ) : null;
    }

    public Type getType()
    {
        return type;
    }

    public enum Type
    {
        PAGE, PAGES, BOOK
    }
}
