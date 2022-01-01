/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.media.items;

import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.api.filesystem.IMount;
import dan200.computercraft.api.media.IMedia;
import dan200.computercraft.core.filesystem.SubMount;
import dan200.computercraft.shared.Registry;
import dan200.computercraft.shared.util.Colour;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.util.List;

public class ItemTreasureDisk extends Item implements IMedia
{
    private static final String NBT_TITLE = "Title";
    private static final String NBT_COLOUR = "Colour";
    private static final String NBT_SUB_PATH = "SubPath";

    public ItemTreasureDisk( Properties settings )
    {
        super( settings );
    }

    @Override
    public void fillItemCategory( @Nonnull CreativeModeTab group, @Nonnull NonNullList<ItemStack> stacks )
    {
    }

    @Override
    public void appendHoverText( @Nonnull ItemStack stack, @Nullable Level world, @Nonnull List<Component> list, @Nonnull TooltipFlag tooltipOptions )
    {
        String label = getTitle( stack );
        if( !label.isEmpty() ) list.add( new TextComponent( label ) );
    }

    @Override
    public boolean doesSneakBypassUse( @Nonnull ItemStack stack, LevelReader world, BlockPos pos, Player player )
    {
        return true;
    }

    @Override
    public String getLabel( @Nonnull ItemStack stack )
    {
        return getTitle( stack );
    }

    @Override
    public IMount createDataMount( @Nonnull ItemStack stack, @Nonnull Level world )
    {
        IMount rootTreasure = getTreasureMount();
        if( rootTreasure == null ) return null;

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

    public static ItemStack create( String subPath, int colourIndex )
    {
        ItemStack result = new ItemStack( Registry.ModItems.TREASURE_DISK.get() );
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

    private static IMount getTreasureMount()
    {
        return ComputerCraftAPI.createResourceMount( "computercraft", "lua/treasure" );
    }

    @Nonnull
    private static String getTitle( @Nonnull ItemStack stack )
    {
        CompoundTag nbt = stack.getTag();
        return nbt != null && nbt.contains( NBT_TITLE ) ? nbt.getString( NBT_TITLE ) : "'missingno' by how did you get this anyway?";
    }

    @Nonnull
    private static String getSubPath( @Nonnull ItemStack stack )
    {
        CompoundTag nbt = stack.getTag();
        return nbt != null && nbt.contains( NBT_SUB_PATH ) ? nbt.getString( NBT_SUB_PATH ) : "dan200/alongtimeago";
    }

    public static int getColour( @Nonnull ItemStack stack )
    {
        CompoundTag nbt = stack.getTag();
        return nbt != null && nbt.contains( NBT_COLOUR ) ? nbt.getInt( NBT_COLOUR ) : Colour.BLUE.getHex();
    }
}
