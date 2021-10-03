/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.util;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;

import javax.annotation.Nonnull;

public class InvisibleSlot extends Slot
{
    public InvisibleSlot(Inventory container, int slot )
    {
        super( container, slot, 0, 0 );
    }

    @Override
    public boolean canInsert( @Nonnull ItemStack stack )
    {
        return false;
    }

    @Override
    public boolean canTakeItems( @Nonnull PlayerEntity player )
    {
        return false;
    }

    @Override
    public boolean isEnabled()
    {
        return false;
    }
}
