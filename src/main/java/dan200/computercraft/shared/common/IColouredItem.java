/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.common;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;

public interface IColouredItem
{
    String NBT_COLOUR = "Color";

    default int getColour( ItemStack stack )
    {
        return getColourBasic( stack );
    }

    static int getColourBasic( ItemStack stack )
    {
    	NbtCompound tag = stack.getNbt();
        return tag != null && tag.contains( NBT_COLOUR ) ? tag.getInt( NBT_COLOUR ) : -1;
    }

    default ItemStack withColour( ItemStack stack, int colour )
    {
        ItemStack copy = stack.copy();
        setColourBasic( copy, colour );
        return copy;
    }

    static void setColourBasic( ItemStack stack, int colour )
    {
        if( colour == -1 )
        {
        	NbtCompound tag = stack.getNbt();
            if( tag != null )
            {
                tag.remove( NBT_COLOUR );
            }
        }
        else
        {
            stack.getOrCreateNbt()
                .putInt( NBT_COLOUR, colour );
        }
    }
}
