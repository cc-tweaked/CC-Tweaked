/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2017. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.computer.items;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.shared.computer.blocks.IComputerTile;
import dan200.computercraft.shared.computer.core.ComputerFamily;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class ItemComputer extends ItemComputerBase
{
    public static int HIGHEST_DAMAGE_VALUE_ID = 16382;

    public ItemComputer( Block block )
    {
        super( block );
        setMaxStackSize( 64 );
        setHasSubtypes( true );
        setUnlocalizedName( "computercraft:computer" );
        setCreativeTab( ComputerCraft.mainCreativeTab );
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

    @Override
    public boolean placeBlockAt( @Nonnull ItemStack stack, @Nonnull EntityPlayer player, World world, @Nonnull BlockPos pos, EnumFacing side, float hitX, float hitY, float hitZ, @Nonnull IBlockState newState )
    {
        if( super.placeBlockAt( stack, player, world, pos, side, hitX, hitY, hitZ, newState ) )
        {
            TileEntity tile = world.getTileEntity( pos );
            if( tile != null && tile instanceof IComputerTile )
            {
                IComputerTile computer = (IComputerTile) tile;
                setupComputerAfterPlacement( stack, computer );
            }
            return true;
        }
        return false;
    }

    private void setupComputerAfterPlacement( @Nonnull ItemStack stack, IComputerTile computer )
    {
        // Set ID
        int id = getComputerID( stack );
        if( id >= 0 )
        {
            computer.setComputerID( id );
        }

        // Set Label
        String label = getLabel( stack );
        if( label != null )
        {
            computer.setLabel( label );
        }
    }

    @Nonnull
    @Override
    public String getUnlocalizedName( @Nonnull ItemStack stack )
    {
        switch( getFamily( stack ) )
        {
            case Normal:
            default:
            {
                return "tile.computercraft:computer";
            }
            case Advanced:
            {
                return "tile.computercraft:advanced_computer";
            }
            case Command:
            {
                return "tile.computercraft:command_computer";
            }
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
        if( (damage & 0x4000) != 0 )
        {
            return ComputerFamily.Advanced;
        }
        return ComputerFamily.Normal;
    }
}
