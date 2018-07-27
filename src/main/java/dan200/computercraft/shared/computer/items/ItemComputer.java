/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2017. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.computer.items;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.shared.computer.blocks.BlockComputer;
import dan200.computercraft.shared.computer.blocks.IComputerTile;
import dan200.computercraft.shared.computer.core.ComputerFamily;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nonnull;

public class ItemComputer extends ItemComputerBase
{
    private final ComputerFamily family;

    public ItemComputer( BlockComputer block )
    {
        super( block );
        family = block.getFamily( 0 );

        setMaxStackSize( 64 );
        setTranslationKey( "computercraft:computer" );
        setCreativeTab( ComputerCraft.mainCreativeTab );
    }

    public ItemStack create( int id, String label )
    {
        // Return the stack
        ItemStack result = new ItemStack( this );
        if( id >= 0 )
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
    public boolean placeBlockAt( @Nonnull ItemStack stack, @Nonnull EntityPlayer player, World world, @Nonnull BlockPos pos, EnumFacing side, float hitX, float hitY, float hitZ, @Nonnull IBlockState newState )
    {
        if( super.placeBlockAt( stack, player, world, pos, side, hitX, hitY, hitZ, newState ) )
        {
            TileEntity tile = world.getTileEntity( pos );
            if( tile instanceof IComputerTile )
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
            return -1;
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
        return family;
    }
}
