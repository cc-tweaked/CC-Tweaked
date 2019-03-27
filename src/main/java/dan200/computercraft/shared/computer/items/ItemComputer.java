/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.computer.items;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.shared.computer.core.ComputerFamily;
import net.minecraft.block.Block;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class ItemComputer extends ItemComputerBase
{
    public static final int HIGHEST_DAMAGE_VALUE_ID = 16382;

    public ItemComputer( Block block )
    {
        super( block );
        setMaxStackSize( 64 );
        setHasSubtypes( true );
        setTranslationKey( "computercraft:computer" );
        setCreativeTab( ComputerCraft.mainCreativeTab );
        addPropertyOverride( new ResourceLocation( ComputerCraft.MOD_ID, "family" ), ( stack, world, player ) ->
            getFamily( stack ) == ComputerFamily.Advanced ? 1 : 0 );
    }

    public ItemStack create( int id, String label, ComputerFamily family )
    {
        // Ignore types we can't handle
        if( family != ComputerFamily.Normal && family != ComputerFamily.Advanced )
        {
            return null;
        }

        // Build the damage
        int damage = 0;
        if( id >= 0 && id <= ItemComputer.HIGHEST_DAMAGE_VALUE_ID )
        {
            damage = id + 1;
        }
        if( family == ComputerFamily.Advanced )
        {
            damage += 0x4000;
        }

        // Return the stack
        ItemStack result = new ItemStack( this, 1, damage );
        if( id > ItemComputer.HIGHEST_DAMAGE_VALUE_ID )
        {
            NBTTagCompound nbt = new NBTTagCompound();
            nbt.setInteger( "computerID", id );
            result.setTagCompound( nbt );
        }
        if( label != null )
        {
            result.setStackDisplayName( label );
        }
        return result;
    }

    @Override
    public void getSubItems( @Nullable CreativeTabs tabs, @Nonnull NonNullList<ItemStack> list )
    {
        if( !isInCreativeTab( tabs ) ) return;
        list.add( ComputerItemFactory.create( -1, null, ComputerFamily.Normal ) );
        list.add( ComputerItemFactory.create( -1, null, ComputerFamily.Advanced ) );
    }

    @Nonnull
    @Override
    public String getTranslationKey( @Nonnull ItemStack stack )
    {
        switch( getFamily( stack ) )
        {
            case Normal:
            default:
                return "tile.computercraft:computer";
            case Advanced:
                return "tile.computercraft:advanced_computer";
            case Command:
                return "tile.computercraft:command_computer";
        }
    }

    // IComputerItem implementation

    @Override
    public int getComputerID( @Nonnull ItemStack stack )
    {
        if( stack.hasTagCompound() && stack.getTagCompound().hasKey( "computerID" ) )
        {
            return stack.getTagCompound().getInteger( "computerID" );
        }
        else
        {
            int damage = stack.getItemDamage() & 0x3fff;
            return (damage - 1);
        }
    }

    @Override
    public ItemStack withFamily( @Nonnull ItemStack stack, @Nonnull ComputerFamily family )
    {
        return ComputerItemFactory.create( getComputerID( stack ), getLabel( stack ), family );
    }

    @Override
    public ComputerFamily getFamily( int damage )
    {
        return (damage & 0x4000) == 0 ? ComputerFamily.Normal : ComputerFamily.Advanced;
    }
}
