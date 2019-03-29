/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.media.items;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.api.filesystem.IMount;
import dan200.computercraft.api.media.IMedia;
import dan200.computercraft.core.filesystem.SubMount;
import dan200.computercraft.shared.util.Colour;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.List;

public class ItemTreasureDisk extends Item implements IMedia
{
    public ItemTreasureDisk()
    {
        setMaxStackSize( 1 );
        setHasSubtypes( true );
        setTranslationKey( "computercraft:treasure_disk" );
    }

    @Override
    public void getSubItems( @Nonnull CreativeTabs tabs, @Nonnull NonNullList<ItemStack> list )
    {
    }

    @Override
    public void addInformation( @Nonnull ItemStack stack, World world, List<String> list, ITooltipFlag flag )
    {
        String label = getTitle( stack );
        if( !label.isEmpty() ) list.add( label );
    }

    @Override
    public boolean doesSneakBypassUse( @Nonnull ItemStack stack, IBlockAccess world, BlockPos pos, EntityPlayer player )
    {
        return true;
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

    public static ItemStack create( String subPath, int colourIndex )
    {
        NBTTagCompound nbt = new NBTTagCompound();
        nbt.setString( "subPath", subPath );

        int slash = subPath.indexOf( '/' );
        if( slash >= 0 )
        {
            String author = subPath.substring( 0, slash );
            String title = subPath.substring( slash + 1 );
            nbt.setString( "title", "\"" + title + "\" by " + author );
        }
        else
        {
            nbt.setString( "title", "untitled" );
        }
        nbt.setInteger( "colour", Colour.values()[colourIndex].getHex() );

        ItemStack result = new ItemStack( ComputerCraft.Items.treasureDisk, 1, 0 );
        result.setTagCompound( nbt );
        return result;
    }

    private static IMount getTreasureMount()
    {
        return ComputerCraftAPI.createResourceMount( ComputerCraft.class, "computercraft", "lua/treasure" );
    }

    // private stuff

    @Nonnull
    private static String getTitle( @Nonnull ItemStack stack )
    {
        NBTTagCompound nbt = stack.getTagCompound();
        return nbt != null && nbt.hasKey( "title" ) ? nbt.getString( "title" ) : "'alongtimeago' by dan200";
    }

    @Nonnull
    private static String getSubPath( @Nonnull ItemStack stack )
    {
        NBTTagCompound nbt = stack.getTagCompound();
        return nbt != null && nbt.hasKey( "subPath" ) ? nbt.getString( "subPath" ) : "dan200/alongtimeago";
    }

    public static int getColour( @Nonnull ItemStack stack )
    {
        NBTTagCompound nbt = stack.getTagCompound();
        return nbt != null && nbt.hasKey( "colour" ) ? nbt.getInteger( "colour" ) : Colour.Blue.getHex();
    }
}
