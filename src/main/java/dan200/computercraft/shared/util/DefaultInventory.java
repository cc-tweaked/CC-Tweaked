/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.util;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;

import javax.annotation.Nonnull;

public interface DefaultInventory extends Inventory
{
    @Override
    default int getMaxCountPerStack()
    {
        return 64;
    }

    @Override
    default void onOpen( @Nonnull PlayerEntity player )
    {
    }

    @Override
    default void onClose( @Nonnull PlayerEntity player )
    {
    }

    @Override
    default boolean isValid( int slot, @Nonnull ItemStack stack )
    {
        return true;
    }
}
