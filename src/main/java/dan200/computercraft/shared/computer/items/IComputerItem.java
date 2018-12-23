/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2017. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.computer.items;

import dan200.computercraft.shared.computer.core.ComputerFamily;
import net.minecraft.item.ItemStack;

import javax.annotation.Nonnull;

public interface IComputerItem
{
    int getComputerID( @Nonnull ItemStack stack );

    String getLabel( @Nonnull ItemStack stack );

    ComputerFamily getFamily( @Nonnull ItemStack stack );

    ItemStack withFamily( @Nonnull ItemStack stack, @Nonnull ComputerFamily family );
}
