/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.util;

import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.container.Slot;
import net.minecraft.item.ItemStack;

public class ValidatingSlot extends Slot
{
    public ValidatingSlot( IInventory inventoryIn, int index, int xPosition, int yPosition )
    {
        super( inventoryIn, index, xPosition, yPosition );
    }

    @Override
    public boolean isItemValid( ItemStack stack )
    {
        return true; // inventory.isItemValidForSlot( slotNumber, stack );
    }
}
