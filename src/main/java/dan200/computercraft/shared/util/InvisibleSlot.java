/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.util;

import javax.annotation.Nonnull;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class InvisibleSlot extends Slot
{
    public InvisibleSlot( Container container, int slot )
    {
        super( container, slot, 0, 0 );
    }

    @Override
    public boolean mayPlace( @Nonnull ItemStack stack )
    {
        return false;
    }

    @Override
    public boolean mayPickup( @Nonnull Player player )
    {
        return false;
    }

    @Override
    public boolean isActive()
    {
        return false;
    }
}
