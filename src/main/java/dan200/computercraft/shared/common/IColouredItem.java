/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.common;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;

public interface IColouredItem
{
    String NBT_COLOUR = "Color";

    default int getColour( ItemStack stack )
    {
        return getColourBasic( stack );
    }

    default ItemStack withColour( ItemStack stack, int colour )
    {
        ItemStack copy = stack.copy();
        setColourBasic( copy, colour );
        return copy;
    }

    static int getColourBasic( ItemStack stack )
    {
        CompoundNBT tag = stack.getTag();
        return tag != null && tag.contains( NBT_COLOUR ) ? tag.getInt( NBT_COLOUR ) : -1;
    }

    static void setColourBasic( ItemStack stack, int colour )
    {
        if( colour == -1 )
        {
            CompoundNBT tag = stack.getTag();
            if( tag != null ) tag.remove( NBT_COLOUR );
        }
        else
        {
            stack.getOrCreateTag().putInt( NBT_COLOUR, colour );
        }
    }
}
