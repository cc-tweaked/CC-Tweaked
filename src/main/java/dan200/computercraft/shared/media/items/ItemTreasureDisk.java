/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.media.items;

import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.api.filesystem.IMount;
import dan200.computercraft.api.media.IMedia;
import dan200.computercraft.core.filesystem.SubMount;
import dan200.computercraft.shared.ComputerCraftRegistry;
import dan200.computercraft.shared.util.Colour;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.util.List;

public class ItemTreasureDisk extends Item implements IMedia
{
    private static final String NBT_TITLE = "Title";
    private static final String NBT_COLOUR = "Colour";
    private static final String NBT_SUB_PATH = "SubPath";

    public ItemTreasureDisk( Settings settings )
    {
        super( settings );
    }

    public static ItemStack create( String subPath, int colourIndex )
    {
        ItemStack result = new ItemStack( ComputerCraftRegistry.ModItems.TREASURE_DISK );
        CompoundTag nbt = result.getOrCreateTag();
        nbt.putString( NBT_SUB_PATH, subPath );

        int slash = subPath.indexOf( '/' );
        if( slash >= 0 )
        {
            String author = subPath.substring( 0, slash );
            String title = subPath.substring( slash + 1 );
            nbt.putString( NBT_TITLE, "\"" + title + "\" by " + author );
        }
        else
        {
            nbt.putString( NBT_TITLE, "untitled" );
        }
        nbt.putInt( NBT_COLOUR, Colour.values()[colourIndex].getHex() );

        return result;
    }

    public static int getColour( @Nonnull ItemStack stack )
    {
        CompoundTag nbt = stack.getTag();
        return nbt != null && nbt.contains( NBT_COLOUR ) ? nbt.getInt( NBT_COLOUR ) : Colour.BLUE.getHex();
    }

    @Override
    public void appendTooltip( @Nonnull ItemStack stack, @Nullable World world, @Nonnull List<Text> list, @Nonnull TooltipContext tooltipOptions )
    {
        String label = getTitle( stack );
        if( !label.isEmpty() )
        {
            list.add( new LiteralText( label ) );
        }
    }

    @Override
    public void appendStacks( @Nonnull ItemGroup group, @Nonnull DefaultedList<ItemStack> stacks )
    {
    }

    @Nonnull
    private static String getTitle( @Nonnull ItemStack stack )
    {
        CompoundTag nbt = stack.getTag();
        return nbt != null && nbt.contains( NBT_TITLE ) ? nbt.getString( NBT_TITLE ) : "'alongtimeago' by dan200";
    }

    @Override
    public String getLabel( @Nonnull ItemStack stack )
    {
        return getTitle( stack );
    }

    @Override
    public IMount createDataMount( @Nonnull ItemStack stack, @Nonnull World world )
    {
        IMount rootTreasure = getTreasureMount();
        String subPath = getSubPath( stack );
        try
        {
            if( rootTreasure.exists( subPath ) )
            {
                return new SubMount( rootTreasure, subPath );
            }
            else if( rootTreasure.exists( "deprecated/" + subPath ) )
            {
                return new SubMount( rootTreasure, "deprecated/" + subPath );
            }
            else
            {
                return null;
            }
        }
        catch( IOException e )
        {
            return null;
        }
    }

    private static IMount getTreasureMount()
    {
        return ComputerCraftAPI.createResourceMount( "computercraft", "lua/treasure" );
    }

    @Nonnull
    private static String getSubPath( @Nonnull ItemStack stack )
    {
        CompoundTag nbt = stack.getTag();
        return nbt != null && nbt.contains( NBT_SUB_PATH ) ? nbt.getString( NBT_SUB_PATH ) : "dan200/alongtimeago";
    }
}
