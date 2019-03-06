/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.computer.items;

import dan200.computercraft.shared.computer.blocks.BlockComputer;
import dan200.computercraft.shared.computer.blocks.TileComputer;
import dan200.computercraft.shared.computer.core.ComputerFamily;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.text.StringTextComponent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class ItemComputer extends ItemComputerBase
{
    private static final String TAG_ID = "computer_id";

    public ItemComputer( BlockComputer block, Settings settings )
    {
        super( block, settings );
    }

    public ItemStack create( int id, String label )
    {
        // Return the stack
        ItemStack result = new ItemStack( this );
        if( id >= 0 ) result.getOrCreateTag().putInt( NBT_ID, id );
        if( label != null ) result.setDisplayName( new StringTextComponent( label ) );
        return result;
    }

    @Override
    protected boolean afterBlockPlaced( BlockPos pos, World world, @Nullable PlayerEntity player, ItemStack stack, BlockState state )
    {
        boolean changed = super.afterBlockPlaced( pos, world, player, stack, state );

        // Sync the ID and label to the computer if needed
        BlockEntity tile = world.getBlockEntity( pos );
        if( !world.isClient && tile instanceof TileComputer )
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
