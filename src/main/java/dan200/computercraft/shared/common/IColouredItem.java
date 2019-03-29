/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.common;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

public interface IColouredItem
{
    String NBT_COLOUR = "colour";

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
        NBTTagCompound tag = stack.getTagCompound();
        return tag != null && tag.hasKey( NBT_COLOUR ) ? tag.getInteger( NBT_COLOUR ) : -1;
    }

    static void setColourBasic( ItemStack stack, int colour )
    {
        NBTTagCompound tag = stack.getTagCompound();
        if( colour == -1 )
        {
            if( tag != null ) tag.removeTag( NBT_COLOUR );
        }
        else
        {
            if( tag == null ) stack.setTagCompound( tag = new NBTTagCompound() );
            tag.setInteger( NBT_COLOUR, colour );
        }
    }
}
