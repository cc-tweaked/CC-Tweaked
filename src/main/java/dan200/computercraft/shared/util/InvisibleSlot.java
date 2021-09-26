/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.util;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.container.Slot;
import net.minecraft.item.ItemStack;

import javax.annotation.Nonnull;

public class InvisibleSlot extends Slot
{
    public InvisibleSlot( IInventory container, int slot )
    {
        super( container, slot, 0, 0 );
    }

    @Override
    public boolean mayPlace( @Nonnull ItemStack stack )
    {
        return false;
    }

    @Override
    public boolean mayPickup( @Nonnull PlayerEntity player )
    {
        return false;
    }

    @Override
    public boolean isActive()
    {
        return false;
    }
}
