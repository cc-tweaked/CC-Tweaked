/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.util;

import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nonnull;

public class ValidatingSlot extends Slot
{
    public ValidatingSlot( Container inventoryIn, int index, int xPosition, int yPosition )
    {
        super( inventoryIn, index, xPosition, yPosition );
    }

    @Override
    public boolean mayPlace( @Nonnull ItemStack stack )
    {
        return true; // inventory.isItemValidForSlot( slotNumber, stack );
    }
}
