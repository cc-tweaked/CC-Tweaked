/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.util;

import net.minecraft.container.Slot;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;

public class ValidatingSlot extends Slot
{
    private final int invSlot;

    public ValidatingSlot( Inventory inventoryIn, int index, int xPosition, int yPosition )
    {
        super( inventoryIn, index, xPosition, yPosition );
        this.invSlot = index;
    }

    @Override
    public boolean canInsert( ItemStack stack )
    {
        return inventory.isValidInvStack( invSlot, stack );
    }
}
