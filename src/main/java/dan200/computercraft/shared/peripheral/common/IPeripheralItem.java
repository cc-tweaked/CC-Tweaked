/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.peripheral.common;

import dan200.computercraft.shared.peripheral.PeripheralType;
import net.minecraft.item.ItemStack;

import javax.annotation.Nonnull;

public interface IPeripheralItem
{
    PeripheralType getPeripheralType( @Nonnull ItemStack stack );
}
