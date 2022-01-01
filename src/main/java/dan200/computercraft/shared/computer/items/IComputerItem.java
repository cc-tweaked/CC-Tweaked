/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.computer.items;

import dan200.computercraft.shared.computer.core.ComputerFamily;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nonnull;

public interface IComputerItem
{
    String NBT_ID = "ComputerId";

    default int getComputerID( @Nonnull ItemStack stack )
    {
        CompoundTag nbt = stack.getTag();
        return nbt != null && nbt.contains( NBT_ID ) ? nbt.getInt( NBT_ID ) : -1;
    }

    default String getLabel( @Nonnull ItemStack stack )
    {
        return stack.hasCustomHoverName() ? stack.getHoverName().getString() : null;
    }

    ComputerFamily getFamily();

    ItemStack withFamily( @Nonnull ItemStack stack, @Nonnull ComputerFamily family );
}
