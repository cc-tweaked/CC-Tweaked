/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.computer.items;

import dan200.computercraft.shared.computer.core.ComputerFamily;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import javax.annotation.Nonnull;

public interface IComputerItem
{
    String NBT_ID = "ComputerId";

    default int getComputerID( @Nonnull ItemStack stack )
    {
        NBTTagCompound tag = stack.getTag();
        return tag != null && tag.contains( NBT_ID ) ? tag.getInt( NBT_ID ) : -1;
    }

    default String getLabel( @Nonnull ItemStack stack )
    {
        return stack.hasDisplayName() ? stack.getDisplayName().getString() : null;
    }

    ComputerFamily getFamily();

    ItemStack withFamily( @Nonnull ItemStack stack, @Nonnull ComputerFamily family );
}
