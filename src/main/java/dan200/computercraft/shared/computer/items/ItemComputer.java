/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.computer.items;

import dan200.computercraft.shared.computer.blocks.BlockComputer;
import dan200.computercraft.shared.computer.blocks.TileComputer;
import dan200.computercraft.shared.computer.core.ComputerFamily;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class ItemComputer extends ItemComputerBase
{
    public ItemComputer( BlockComputer block, Properties settings )
    {
        super( block, settings );
    }

    public ItemStack create( int id, String label )
    {
        // Return the stack
        ItemStack result = new ItemStack( this );
        if( id >= 0 ) result.getOrCreateTag().putInt( NBT_ID, id );
        if( label != null ) result.setDisplayName( new TextComponentString( label ) );
        return result;
    }

    @Override
    protected boolean onBlockPlaced( @Nonnull BlockPos pos, World world, @Nullable EntityPlayer player, @Nonnull ItemStack stack, IBlockState state )
    {
        boolean changed = super.onBlockPlaced( pos, world, player, stack, state );

        // Sync the ID and label to the computer if needed
        TileEntity tile = world.getTileEntity( pos );
        if( !world.isRemote && tile instanceof TileComputer )
        {
            TileComputer computer = (TileComputer) tile;
            computer.setComputerID( getComputerID( stack ) );
            computer.setLabel( getLabel( stack ) );
            changed = true;
        }

        return changed;
    }

    @Override
    public ItemStack withFamily( @Nonnull ItemStack stack, @Nonnull ComputerFamily family )
    {
        ItemStack result = ComputerItemFactory.create( getComputerID( stack ), null, family );
        if( stack.hasDisplayName() ) result.setDisplayName( stack.getDisplayName() );
        return result;
    }
}
