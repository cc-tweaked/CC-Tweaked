/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.computer.items;

import dan200.computercraft.shared.computer.blocks.BlockComputer;
import dan200.computercraft.shared.computer.core.ComputerFamily;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nonnull;

public class ItemComputer extends ItemComputerBase
{
    public ItemComputer( BlockComputer<?> block, Properties settings )
    {
        super( block, settings );
    }

    public ItemStack create( int id, String label )
    {
        ItemStack result = new ItemStack( this );
        if( id >= 0 ) result.getOrCreateTag().putInt( NBT_ID, id );
        if( label != null ) result.setHoverName( new TextComponent( label ) );
        return result;
    }

    @Override
    public ItemStack withFamily( @Nonnull ItemStack stack, @Nonnull ComputerFamily family )
    {
        ItemStack result = ComputerItemFactory.create( getComputerID( stack ), null, family );
        if( stack.hasCustomHoverName() ) result.setHoverName( stack.getHoverName() );
        return result;
    }
}
