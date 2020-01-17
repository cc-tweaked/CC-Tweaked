/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.computer.items;

import dan200.computercraft.shared.computer.core.ComputerFamily;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import javax.annotation.Nonnull;

public interface IComputerItem
{
    String NBT_ID = "computerID";

    default int getComputerID( @Nonnull ItemStack stack )
    {
        NBTTagCompound nbt = stack.getTagCompound();
        return nbt != null && nbt.hasKey( NBT_ID ) ? nbt.getInteger( NBT_ID ) : -1;
    }

    default String getLabel( @Nonnull ItemStack stack )
    {
        return stack.hasDisplayName() ? stack.getDisplayName() : null;
    }

    ComputerFamily getFamily( @Nonnull ItemStack stack );

    ItemStack withFamily( @Nonnull ItemStack stack, @Nonnull ComputerFamily family );
}
